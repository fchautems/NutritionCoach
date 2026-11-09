const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {resolve} = require('node:path');
const {test} = require('node:test');
const {JSDOM, VirtualConsole} = require('jsdom');

const documentHtml = readFileSync(resolve(__dirname, '../app/src/main/assets/nutritioncoach.html'), 'utf8')
    .replace('<!-- nutrition-boot -->', '<script>window.nutritionOpening = 17;</script>');
const today = '2026-11-07';
const oldState = {
    profile: {name: 'Camille', sex: 'female', height: 175, weight: 78.5},
    goal: 2175,
    entries: [
        {id: 'old', date: today, name: 'Pomme', kcal: 52},
        {id: 'lunch', date: today, meal: 'lunch', name: 'Riz cuit (150 g)', kcal: 195}
    ]
};

function openApp(t, {data = structuredClone(oldState), detailed = true, day = today, withings = false,
    readWithings} = {}) {
    const errors = [];
    const virtualConsole = new VirtualConsole();
    virtualConsole.on('jsdomError', error => errors.push(error));
    const dom = new JSDOM(documentHtml, {
        url: 'https://appassets.androidplatform.net/',
        runScripts: 'dangerously',
        virtualConsole,
        beforeParse(window) {
            const [year, month, date] = day.split('-').map(Number);
            window.Date = class extends Date {
                constructor(...args) { super(...(args.length ? args : [year, month - 1, date, 12])); }
            };
            window.scrollTo = () => {};
            const modalOpeners = new WeakMap();
            window.HTMLDialogElement.prototype.showModal = function () {
                modalOpeners.set(this, window.document.activeElement);
                this.open = true;
                this.querySelector('[autofocus]')?.focus();
            };
            window.HTMLDialogElement.prototype.close = function () {
                this.open = false;
                modalOpeners.get(this)?.focus();
            };
            window.NutritionHost = {
                state: () => JSON.stringify(data),
                ready: opening => { window.readyOpening = opening; },
                updateInfo: () => '{"version":4,"status":"idle"}',
                saveProfile: (name, sex, height, weight) => {
                    data.profile = {name, sex, height: Number(height), weight: Number(weight)};
                    return true;
                },
                saveGoal: value => { data.goal = Number(value); return true; },
                addEntry: (date, meal, name, kcal) => {
                    data.entries.push({id: 'added', date, meal, name, kcal: Number(kcal)});
                    return true;
                },
                deleteEntry: id => {
                    const index = data.entries.findIndex(entry => entry.id === id);
                    if (index === -1) return false;
                    data.entries.splice(index, 1);
                    return true;
                }
            };
            if (withings) {
                window.NutritionHost.withingsInfo = () => JSON.stringify({
                    available: true, status: data.withingsStatus || 'idle', added: data.withingsAdded ?? 1
                });
                window.NutritionHost.importWithings = () => {
                    const notify = () => window.dispatchEvent(new window.Event('nutrition-withings'));
                    if (readWithings) {
                        readWithings(notify);
                        return;
                    }
                    data.withingsStatus = 'done';
                    data.withingsWeights = {
                        old: {id: 'old', time: Date.UTC(2026, 9, 24, 8), day: '2026-10-24', kg: 78.12},
                        latest: {id: 'latest', time: Date.UTC(2026, 10, 7, 8), day: today, kg: 76.12}
                    };
                    notify();
                };
            }
            if (detailed) {
                window.NutritionHost.saveProfileDetails = payload => {
                    const draft = JSON.parse(payload);
                    data.profile = {...data.profile, ...draft, height: Number(draft.height),
                        weight: Number(draft.weight.replace(',', '.')),
                        targetWeight: Number(draft.targetWeight.replace(',', '.'))};
                    return true;
                };
            }
        }
    });
    t.after(() => {
        dom.window.close();
        assert.deepEqual(errors, []);
    });
    assert.equal(dom.window.readyOpening, 17);
    const document = dom.window.document;
    const field = id => document.getElementById(id);
    return {
        field, data, document,
        back: () => dom.window.nutritionBack(),
        visible: id => !field(id).closest('[hidden]'),
        text: id => field(id).textContent,
        click: id => field(id).click(),
        fill(id, value) {
            field(id).value = value;
            field(id).dispatchEvent(new dom.window.Event('input', {bubbles: true}));
        },
        submit: id => field(id).requestSubmit(),
        editProfile() {
            field('settings-button').click();
            field('edit-profile').click();
        }
    };
}

test('reprend le profil, le journal et l’objectif, puis persiste la date de naissance', t => {
    const app = openApp(t);
    assert.match(app.text('total'), /^247 kcal$/);
    assert.match(app.text('legacy-entries'), /Pomme/);
    assert.match(app.text('goal-button'), /2\s175 kcal/);
    assert.equal(app.visible('birthday'), false);
    app.editProfile();
    assert.equal(app.field('first-name').value, 'Camille');
    assert.equal(app.field('birth-date').value, '');
    assert.equal(app.field('weight').value, '78,5');
    assert.match(app.text('profile-bmi'), /25[.,]6/);
    app.fill('birth-date', '1988-11-07');
    assert.match(app.text('birth-age'), /38 ans/);
    app.fill('movement', 'mixed');
    app.fill('sport', 'none');
    const noSportEnergy = app.text('profile-energy');
    app.fill('sport', 'regular');
    assert.match(app.text('profile-energy'), /estimée · ≈ 2\s550 kcal\/j/);
    assert.notEqual(app.text('profile-energy'), noSportEnergy);
    assert.match(app.text('energy-goal'), /2\s175 kcal\/j se situe en dessous/);
    app.fill('weight-goal-mode', 'maintain');
    assert.match(app.text('weight-pace'), /0 kg\/semaine/);
    app.submit('profile-form');

    const reopened = openApp(t, {data: app.data});
    assert.match(reopened.text('birthday'), /Joyeux anniversaire, Camille/);
    assert.equal(reopened.visible('birthday'), true);
    reopened.click('previous');
    assert.equal(reopened.visible('birthday'), false);
    reopened.click('return-today');
    assert.match(reopened.text('total'), /^247 kcal$/);
    assert.match(reopened.text('goal-button'), /2\s175 kcal/);
    reopened.click('goal-button');
    reopened.fill('goal-input', '2230');
    reopened.submit('goal-form');
    reopened.editProfile();
    assert.equal(reopened.field('birth-date').value, '1988-11-07');
    assert.equal(reopened.field('weight-goal-mode').value, 'maintain');
    assert.match(reopened.text('energy-goal'), /2\s230 kcal\/j/);
});

test('compare les rythmes et permet de modifier une échéance serrée ou le poids cible', t => {
    const data = {...structuredClone(oldState), profile: {...oldState.profile,
        sex: 'undisclosed', birthDate: '1988-11-08', movement: 'mixed', sport: 'regular',
        weighIns: {'2026-10-24': 80.5, [today]: 78.5}}};
    const app = openApp(t, {data});
    assert.equal(app.visible('birthday'), false);
    app.editProfile();
    assert.match(app.text('birth-age'), /37 ans/);
    assert.match(app.text('profile-energy'), /≈ [\d\s]+ à [\d\s]+ kcal\/j/);
    app.fill('weight-goal-mode', 'target');
    app.fill('target-weight', '72,5');
    assert.match(app.text('weight-pace'), /Ajoute une échéance/);
    assert.equal(app.field('weight-guide').open, false);
    app.field('weight-guide').querySelector('summary').click();
    assert.equal(app.field('weight-guide').open, true);
    assert.match(app.text('weight-range'), /56[.,]7 à 76[.,]3 kg/);
    app.fill('weight-deadline', '2026-11-14');
    assert.match(app.text('weight-pace'), /−6 kg\/semaine/);
    assert.match(app.text('observed-pace'), /−1 kg\/semaine sur 14 jours.*plus vite/);
    assert.equal(app.visible('deadline-actions'), true);
    app.click('adjust-weight');
    assert.equal(app.document.activeElement.id, 'target-weight');
    app.click('postpone-deadline');
    assert.equal(app.field('weight-deadline').value, '2026-12-19');
    assert.match(app.text('observed-pace'), /proche du rythme visé/);
    assert.equal(app.visible('deadline-actions'), false);
    app.fill('weight-deadline', today);
    assert.match(app.text('pace-warning'), /déjà passée/);
    app.fill('weight-deadline', '');
    app.submit('profile-form');
    app.editProfile();
    assert.equal(app.field('target-weight').value, '72,5');
    assert.equal(app.field('weight-deadline').value, '');
});

test('garde le journal et le catalogue utilisables avec le pont de la V0.3', t => {
    const app = openApp(t, {detailed: false});
    app.editProfile();
    app.click('settings-button');
    assert.equal(app.visible('import-withings'), false);
    app.field('settings').querySelector('.close').click();
    assert.equal(app.visible('birth-date'), false);
    assert.equal(app.visible('weight-goal-mode'), false);
    app.fill('first-name', 'Camille bis');
    app.submit('profile-form');
    app.field('meals').querySelectorAll('button')[1].click();
    assert.match(app.text('meal-entries'), /Riz cuit/);
    app.fill('food-search', 'chocolat');
    app.field('food-results').querySelector('button').click();
    app.fill('grams', '20');
    app.submit('portion-form');
    assert.match(app.text('meal-entries'), /Chocolat noir/);
    app.click('back');
    assert.match(app.text('total'), /^354 kcal$/);
    assert.match(app.text('goal-button'), /2\s175 kcal/);
});

test('un premier profil peut ouvrir le journal sans date de naissance ni échéance', t => {
    const app = openApp(t, {data: {profile: null, goal: 2000, entries: []}});
    assert.equal(app.visible('profile'), true);
    app.fill('first-name', 'Camille');
    app.fill('sex', 'other');
    app.fill('height', '175');
    app.fill('weight', '78,5');
    app.fill('birth-date', '2010-11-07');
    assert.match(app.text('energy-goal'), /prévue pour les adultes/);
    app.fill('weight-goal-mode', 'target');
    assert.equal(app.visible('weight-guide'), false);
    app.fill('weight-goal-mode', '');
    app.fill('birth-date', '');
    app.submit('profile-form');
    assert.equal(app.visible('today'), true);
    assert.equal(app.visible('birthday'), false);
    assert.equal(app.data.profile.weight, 78.5);
});

test('la proposition calorique modifie le journal seulement après confirmation', t => {
    const data = {...structuredClone(oldState), profile: {...oldState.profile,
        birthDate: '1988-11-07', movement: 'mixed', sport: 'regular', goalMode: 'target', targetWeight: 73}};
    const app = openApp(t, {data});
    app.editProfile();
    assert.match(app.text('calorie-proposal-title'), /2\s300 kcal\/j/);
    app.fill('weight-deadline', '2026-11-14');
    assert.match(app.text('calorie-proposal-title'), /2\s050 kcal\/j/);
    assert.match(app.text('calorie-proposal-detail'), /ajustement est limité/);
    app.fill('weight-deadline', today);
    assert.doesNotMatch(app.text('calorie-proposal-title'), /kcal/);
    app.fill('weight-deadline', '');
    app.fill('target-weight', '55');
    assert.doesNotMatch(app.text('calorie-proposal-title'), /kcal/);
    app.fill('target-weight', '73');
    app.submit('profile-form');
    assert.equal(app.field('goal-dialog').open, true);
    assert.equal(app.field('goal-input').value, '2300');
    assert.match(app.text('goal-proposal-note'), /2\s175 kcal\/j/);
    assert.equal(app.text('goal-save'), 'Appliquer au journal');
    assert.equal(app.text('goal-cancel'), 'Garder l’objectif actuel');
    assert.equal(data.goal, 2175);
    app.field('goal-dialog').querySelector('.close').click();
    assert.equal(data.goal, 2175);
    app.editProfile();
    app.submit('profile-form');
    app.submit('goal-form');
    assert.equal(data.goal, 2300);
    assert.match(app.text('goal-button'), /2\s300 kcal/);
    assert.match(app.text('total'), /^247 kcal$/);
});

test('les portions utilisent les valeurs propres à l’état et acceptent une quantité décimale', t => {
    const app = openApp(t);
    app.field('meals').querySelectorAll('button')[1].click();
    app.fill('food-search', 'pâtes sans œufs');
    const results = app.field('food-results').querySelectorAll('button');
    assert.equal(results.length, 1);
    results[0].click();
    assert.match(app.text('weighing-state'), /Sec · avant cuisson/);
    assert.equal(app.field('grams').value, '75');
    assert.match(app.text('portion-total'), /265 kcal/);
    app.field('weighing-state').value = '1';
    app.field('weighing-state').dispatchEvent(new app.document.defaultView.Event('change'));
    assert.equal(app.field('grams').value, '170');
    assert.match(app.text('portion-total'), /269 kcal/);
    app.submit('portion-form');
    assert.equal(app.data.entries.at(-1).name, 'Pâtes sans œufs · cuit · 170 g');
    assert.equal(app.data.entries.at(-1).kcal, 269);
    app.fill('food-search', 'huile olive');
    app.field('food-results').querySelector('button').click();
    app.field('portion-presets').querySelector('button').click();
    assert.equal(app.field('grams').value, '4.6');
    assert.match(app.text('portion-total'), /41 kcal/);
    app.submit('portion-form');
    assert.equal(app.data.entries.at(-1).kcal, 41);
});

test('trouve les nouveaux plats et boissons et calcule le riz complet dans les deux états', t => {
    const app = openApp(t);
    app.field('meals').querySelectorAll('button')[1].click();
    const category = name => app.field('food-categories').querySelector(`[data-group="${name}"]`);
    const result = name => [...app.field('food-results').querySelectorAll('button')].find(
        button => button.querySelector('span').firstChild.textContent === name);
    category('dishes').click();
    assert.match(result('Lasagnes à la viande').textContent, /154 kcal/);
    assert.ok(result('Pizza margherita'));
    result('Lasagnes à la viande').click();
    assert.equal(app.field('grams').value, '250');
    assert.match(app.text('portion-total'), /385 kcal/);
    app.submit('portion-form');
    assert.deepEqual(app.data.entries.at(-1), {
        id: 'added', date: today, meal: 'lunch', name: 'Lasagnes à la viande · 250 g', kcal: 385
    });
    app.click('reset-catalog');
    assert.match(app.text('recent-food-list'), /Lasagnes à la viande/);
    category('drinks').click();
    assert.match(result('Boisson à l’avoine').textContent, /47 kcal/);
    assert.ok(result('Jus d’orange'));
    app.fill('food-search', 'riz complet');
    result('Riz complet').click();
    assert.match(app.text('portion-total'), /160 kcal/);
    const weighing = app.field('weighing-state');
    weighing.value = '1';
    weighing.dispatchEvent(new app.document.defaultView.Event('change'));
    assert.equal(app.field('grams').value, '60');
    assert.match(app.text('portion-total'), /210 kcal/);
    assert.equal(app.data.entries.length, oldState.entries.length + 1);
});

test('importe les pesées, affiche la tendance et propose de reprendre le dernier poids', t => {
    const data = {...structuredClone(oldState), profile: {...oldState.profile, goalMode: 'maintain',
        weighIns: {[today]: 76.12}}};
    const app = openApp(t, {data, withings: true});
    app.click('settings-button');
    assert.equal(app.visible('import-withings'), true);
    app.click('import-withings');
    assert.match(app.text('withings-status'), /nouvelle pesée importée/);
    data.withingsAdded = 0;
    app.click('import-withings');
    assert.match(app.text('withings-status'), /Aucune nouvelle pesée Withings trouvée dans Health Connect/);
    app.click('edit-profile');
    assert.match(app.text('withings-latest'), /Dernière pesée importée de Withings · 76[.,]12 kg/);
    assert.match(app.text('observed-pace'), /−1 kg\/semaine/);
    assert.equal(app.field('weight').value, '78,5');
    app.click('use-withings-weight');
    assert.equal(app.field('weight').value, '76,12');
    app.submit('profile-form');
    assert.equal(app.data.profile.withingsId, 'latest');
    assert.equal(app.data.profile.weight, 76.12);
});

test('distingue le résultat de la lecture Withings de l’historique enregistré', t => {
    const data = {...structuredClone(oldState), withingsStatus: 'done', withingsAdded: 1, withingsWeights: {
        old: {id: 'old', time: Date.UTC(2026, 1, 1, 8), day: '2026-02-01', kg: 63.531}
    }};
    let notify;
    const app = openApp(t, {data, withings: true, readWithings: callback => { notify = callback; }});
    app.click('settings-button');
    app.click('withings-diagnostic');
    assert.equal(app.field('settings').open, false);
    assert.equal(app.field('withings-diagnostic-dialog').open, true);
    assert.match(app.text('withings-diagnostic-result'), /en cours/);
    assert.equal(app.field('withings-diagnostic-retry').disabled, true);
    const history = app.text('withings-diagnostic-history');
    assert.match(history, /1 pesée enregistrée.*01\.02\.2026/);

    data.withingsStatus = 'empty';
    notify();
    assert.match(app.text('withings-diagnostic-result'), /Aucune pesée Withings lisible/);
    assert.equal(app.text('withings-diagnostic-history'), history);

    app.click('withings-diagnostic-retry');
    data.withingsStatus = 'done';
    data.withingsAdded = 0;
    notify();
    assert.match(app.text('withings-diagnostic-result'), /a renvoyé des pesées.*Aucune nouvelle/);
    assert.equal(app.text('withings-diagnostic-history'), history);

    app.click('withings-diagnostic-retry');
    data.withingsAdded = 1;
    data.withingsWeights.latest = {id: 'latest', time: Date.UTC(2026, 10, 7, 8), day: today, kg: 65};
    notify();
    assert.match(app.text('withings-diagnostic-result'), /1 nouvelle pesée ajoutée/);
    assert.match(app.text('withings-diagnostic-history'), /2 pesées enregistrées.*07\.11\.2026/);
});

test('affiche les 243 pesées importées par date depuis le diagnostic et le profil', t => {
    const data = structuredClone(oldState);
    const days = Array.from({length: 243}, (_, index) => {
        const time = Date.UTC(2026, 1, 1 - index, 8);
        return {id: 'record-' + index, time, day: new Date(time).toISOString().slice(0, 10), kg: 65};
    });
    const unordered = [...days.slice(100), ...days.slice(0, 100).reverse()];
    data.withingsWeights = Object.fromEntries(unordered.map(record => [record.id, record]));
    const app = openApp(t, {data, withings: true, readWithings: () => {}});

    app.click('settings-button');
    app.click('withings-diagnostic');
    app.click('diagnostic-withings-history');
    assert.equal(app.field('withings-history').open, true);
    assert.match(app.text('withings-history-count'), /243 pesées enregistrées/);
    const visibleDays = () => Array.from(app.field('withings-weights').querySelectorAll('time'), item => item.dateTime);
    assert.deepEqual(visibleDays(), days.map(record => record.day));
    assert.match(app.field('withings-weights').firstElementChild.textContent, /01\.02\.2026.*65 kg/);

    assert.equal(app.back(), true);
    assert.equal(app.field('withings-history').open, false);
    assert.equal(app.field('withings-diagnostic-dialog').open, true);
    app.back();
    app.editProfile();
    app.click('profile-withings-history');
    assert.equal(app.field('withings-history').open, true);
    assert.deepEqual(visibleDays(), days.map(record => record.day));
});

test('ouvre le bon repas en un appui et ajoute sur le jour affiché', t => {
    const app = openApp(t);
    const meals = () => [...app.field('meals').querySelectorAll('button')];
    assert.equal(app.visible('quick-meal-title'), true);
    assert.deepEqual(meals().map(button => button.querySelector('.meal-label').firstChild.textContent),
        ['Petit-déjeuner', 'Midi', 'Soir']);
    assert.equal(meals()[1].querySelector('small').textContent, '195 kcal');

    app.click('previous');
    meals()[2].click();
    assert.equal(app.text('meal-title'), 'Soir');
    assert.equal(app.document.activeElement.id, 'meal-title');
    assert.equal(app.visible('food-search'), true);
    app.fill('food-search', 'chocolat');
    app.field('food-results').querySelector('button').click();
    app.fill('grams', '20');
    app.submit('portion-form');
    app.click('back');

    assert.equal(app.field('day').dateTime, '2026-11-06');
    assert.equal(meals()[2].querySelector('small').textContent, '107 kcal');
    assert.equal(app.document.activeElement, meals()[2]);
    assert.deepEqual(app.data, {...oldState, entries: [...oldState.entries, {
        id: 'added', date: '2026-11-06', meal: 'dinner', name: 'Chocolat noir · 20 g', kcal: 107
    }]});
    app.click('return-today');
    assert.equal(app.text('total'), '247 kcal');

    meals()[1].click();
    app.fill('food-search', 'riz');
    assert.equal(app.back(), true);
    assert.equal(app.visible('meal'), true);
    assert.equal(app.visible('food-categories'), true);
    assert.equal(app.back(), true);
    assert.equal(app.document.activeElement, meals()[1]);
    assert.equal(app.field('day').dateTime, today);
});

test('ouvre directement l’état de pesée recherché et garde les valeurs du journal', t => {
    const data = structuredClone(oldState);
    const before = structuredClone(data);
    const app = openApp(t, {data});
    app.field('meals').querySelectorAll('button')[1].click();
    app.fill('food-search', 'pates sans oeufs cuit');
    const cooked = app.field('food-results').querySelector('button');
    assert.match(cooked.textContent, /158 kcal \/ 100 g · cuit/);
    cooked.click();
    assert.equal(app.field('weighing-state').value, '1');
    assert.equal(app.field('grams').value, '170');
    assert.deepEqual(data, before);

    app.fill('grams', '150');
    app.submit('portion-form');
    assert.deepEqual(data, {...before, entries: [...before.entries, {
        id: 'added', date: today, meal: 'lunch', name: 'Pâtes sans œufs · cuit · 150 g', kcal: 237
    }]});

    app.fill('food-search', 'pâtes cuites');
    assert.equal(app.field('food-results').querySelectorAll('button').length, 2);
    app.fill('food-search', 'pâtes sèches assiette');
    assert.equal(app.visible('food-empty'), true);
    app.fill('food-search', 'boeuf hache cru');
    const raw = app.field('food-results').querySelector('button');
    assert.match(raw.textContent, /208 kcal \/ 100 g · cru/);
    raw.click();
    assert.equal(app.field('weighing-state').value, '1');
    assert.equal(app.field('grams').value, '125');
    app.field('portion-dialog').querySelector('.close').click();
    assert.equal(data.entries.length, before.entries.length + 1);
});

test('retrouve des aliments du catalogue par leur nom courant', t => {
    const app = openApp(t, {detailed: false});
    app.field('meals').querySelectorAll('button')[1].click();
    app.fill('food-search', '  POMME  ');
    const [first, ...others] = app.field('food-results').querySelectorAll('button');
    assert.equal(first.querySelector('span').firstChild.textContent, 'Pomme');
    assert.equal(first.querySelector('small').textContent, '52 kcal / 100 g');
    assert.ok(others.some(row => row.textContent.startsWith('Laitue pommée')));
    assert.ok(others.some(row => row.textContent.startsWith('Pommes de terre')));
    first.click();
    assert.equal(app.text('portion-title'), 'Pomme');
    assert.equal(app.field('grams').value, '150');
    assert.match(app.text('portion-total'), /78 kcal/);
    app.field('portion-dialog').querySelector('.close').click();

    for (const [query, name, kcal] of [
        ['patates cuites', 'Pommes de terre nouvelles', 66],
        ['filet de poulet', 'Blanc de poulet grillé', 128],
        ['œufs durs', 'Œuf dur', 156]
    ]) {
        app.fill('food-search', query);
        const rows = app.field('food-results').querySelectorAll('button');
        assert.equal(rows.length, query === 'patates cuites' ? 3 : 1, query);
        assert.ok(rows[0].textContent.includes(name), query);
        assert.ok(rows[0].textContent.includes(kcal + ' kcal / 100 g'), query);
    }
    for (const query of ['filet de poulet cru']) {
        app.fill('food-search', query);
        assert.equal(app.visible('food-empty'), true, query);
    }
    assert.deepEqual(app.data, oldState);
});

test('ouvre directement une date du journal et saisit dans le jour sélectionné', t => {
    const data = structuredClone(oldState);
    const past = '2026-10-25';
    const previousDay = '2026-10-24';
    data.entries.push({id: 'past', date: past, meal: 'lunch', name: 'Poire', kcal: 75});
    data.profile.targetWeight = 74;
    data.profile.weighIns = {[past]: 79.2, [today]: 78.5};
    const before = structuredClone(data);
    const app = openApp(t, {data, detailed: false});

    app.click('choose-day');
    assert.equal(app.field('day-dialog').open, true);
    assert.equal(app.field('day-input').value, today);
    assert.equal(app.document.activeElement.id, 'day-input');
    app.fill('day-input', past);
    assert.equal(app.back(), true);
    assert.equal(app.field('day-dialog').open, false);
    assert.equal(app.field('day').dateTime, today);

    app.click('choose-day');
    assert.equal(app.field('day-input').value, today);
    app.fill('day-input', '');
    app.submit('day-form');
    assert.equal(app.field('day-dialog').open, true);
    assert.equal(app.field('day').dateTime, today);
    app.fill('day-input', past);
    app.submit('day-form');
    assert.equal(app.field('day-dialog').open, false);
    assert.equal(app.field('day').dateTime, past);
    assert.equal(app.text('total'), '75 kcal');
    app.click('previous');
    assert.equal(app.field('day').dateTime, previousDay);
    app.click('next');
    assert.equal(app.field('day').dateTime, past);
    assert.deepEqual(data, before);

    app.field('meals').querySelectorAll('button')[1].click();
    assert.match(app.text('meal-entries'), /Poire/);
    app.fill('food-search', 'pomme');
    app.field('food-results').querySelector('button').click();
    assert.equal(app.field('portion-dialog').getAttribute('aria-describedby'), 'portion-destination');
    assert.match(app.text('portion-destination'), /^Midi · .*25 octobre 2026$/);
    assert.equal(app.back(), true);
    assert.deepEqual(data, before);

    app.click('manual-open');
    app.fill('food', 'Soupe');
    app.fill('kcal', '120');
    app.submit('entry-form');
    app.click('back');
    assert.equal(app.text('total'), '195 kcal');
    app.click('return-today');
    assert.equal(app.field('day').dateTime, today);
    assert.equal(app.text('total'), '247 kcal');
    assert.deepEqual(data, {...before, entries: [...before.entries,
        {id: 'added', date: past, meal: 'lunch', name: 'Soupe', kcal: 120}]});

    app.field('meals').querySelectorAll('button')[2].click();
    app.fill('food-search', 'pomme');
    app.field('food-results').querySelector('button').click();
    assert.match(app.text('portion-destination'), /^Soir · .*7 novembre 2026$/);
});

test('efface une recherche en un appui et revient à la catégorie sans ouvrir le clavier', t => {
    const data = structuredClone(oldState);
    data.profile.weighIns = {[today]: 78.5};
    data.withingsWeights = {old: {day: '2026-11-01', kg: 78}};
    const before = structuredClone(data);
    const app = openApp(t, {data});
    app.field('meals').querySelectorAll('button')[1].click();

    const search = app.field('food-search');
    const category = app.field('food-categories').querySelector('[data-group="starches"]');
    assert.equal(app.visible('clear-food-search'), false);
    category.click();
    assert.equal(app.visible('reset-catalog'), true);
    app.click('reset-catalog');
    assert.equal(app.document.activeElement, category);
    assert.equal(app.visible('food-categories'), true);
    category.click();
    assert.equal(app.back(), true);
    assert.equal(app.document.activeElement, category);

    app.fill('food-search', 'sans correspondance');
    assert.equal(app.visible('food-empty'), true);
    assert.equal(app.visible('reset-catalog'), false);
    assert.equal(app.visible('clear-food-search'), true);
    app.click('clear-food-search');
    assert.equal(search.value, '');
    assert.equal(app.document.activeElement, search);
    assert.equal(app.visible('food-categories'), true);
    assert.equal(app.visible('clear-food-search'), false);

    app.fill('food-search', 'pomme');
    assert.match(app.text('food-results'), /Pomme/);
    app.fill('food-search', '…');
    assert.equal(app.visible('clear-food-search'), true);
    assert.equal(app.back(), true);
    assert.equal(app.visible('meal'), true);
    assert.equal(search.value, '');
    assert.equal(app.document.activeElement, app.field('catalog-title'));
    assert.deepEqual(data, before);
});

test('reprend une recherche sans résultat en saisie manuelle et préserve le brouillon', t => {
    const app = openApp(t, {detailed: false});
    app.click('previous');
    app.field('meals').querySelectorAll('button')[2].click();

    app.fill('food-search', 'pomme');
    app.click('manual-open');
    assert.equal(app.field('food').value, '');
    assert.equal(app.document.activeElement, app.field('food'));
    app.click('manual-open');

    app.fill('food-search', '  Galette maison  ');
    app.click('manual-open');
    assert.equal(app.field('food').value, 'Galette maison');
    assert.equal(app.document.activeElement, app.field('kcal'));
    assert.deepEqual(app.data, oldState);

    app.fill('kcal', '230');
    app.click('manual-open');
    app.fill('food-search', 'Soupe maison');
    app.click('manual-open');
    assert.equal(app.field('food').value, 'Galette maison');
    assert.equal(app.field('kcal').value, '230');
    app.submit('entry-form');
    assert.deepEqual(app.data, {...oldState, entries: [...oldState.entries, {
        id: 'added', date: '2026-11-06', meal: 'dinner', name: 'Galette maison', kcal: 230
    }]});

    app.click('manual-open');
    app.fill('food-search', 'a'.repeat(81));
    app.click('manual-open');
    assert.equal(app.field('food').value, '');
    assert.equal(app.document.activeElement, app.field('food'));
});

test('le retour Android replie la saisie manuelle avant de quitter la recherche ou le repas', t => {
    const data = structuredClone(oldState);
    data.profile.weighIns = {[today]: 78.5};
    data.withingsWeights = {old: {day: '2026-11-01', kg: 78}};
    const before = structuredClone(data);
    const app = openApp(t, {data, detailed: false});
    app.click('previous');
    app.field('meals').querySelectorAll('button')[2].click();
    const manual = app.field('manual-open');
    const search = app.field('food-search');

    app.click('manual-open');
    app.fill('food', 'Galette maison');
    app.fill('kcal', '230');
    assert.equal(app.back(), true);
    assert.equal(app.visible('meal'), true);
    assert.equal(app.visible('entry-form'), false);
    assert.equal(manual.getAttribute('aria-expanded'), 'false');
    assert.equal(app.document.activeElement, manual);
    assert.deepEqual(data, before);

    app.fill('food-search', 'Soupe maison');
    app.click('manual-open');
    assert.equal(app.field('food').value, 'Galette maison');
    assert.equal(app.field('kcal').value, '230');
    assert.equal(app.back(), true);
    assert.equal(app.visible('entry-form'), false);
    assert.equal(app.visible('catalog-results'), true);
    assert.equal(search.value, 'Soupe maison');

    assert.equal(app.back(), true);
    assert.equal(app.visible('food-categories'), true);
    assert.equal(search.value, '');
    app.click('manual-open');
    assert.equal(app.field('food').value, 'Galette maison');
    assert.equal(app.field('kcal').value, '230');
    assert.deepEqual(data, before);
    app.submit('entry-form');
    assert.deepEqual(data, {...before, entries: [...before.entries, {
        id: 'added', date: '2026-11-06', meal: 'dinner', name: 'Galette maison', kcal: 230
    }]});
});

test('accepte une virgule dans la quantité et bloque les portions invalides', t => {
    const app = openApp(t, {detailed: false});
    app.field('meals').querySelectorAll('button')[1].click();
    app.fill('food-search', 'huile olive');
    app.field('food-results').querySelector('button').click();
    assert.equal(app.field('grams').inputMode, 'decimal');
    const before = structuredClone(app.data);
    app.fill('grams', '4,6');
    assert.match(app.text('portion-total'), /41 kcal/);
    for (const invalid of ['', '0,9', '2000,1', '4,65', '4,', '4,6 kg', '4,6 ml', '2e2', '2000']) {
        app.fill('grams', invalid);
        assert.equal(app.field('add-portion').disabled, true, invalid);
        app.submit('portion-form');
    }
    assert.deepEqual(app.data, before);
    app.fill('grams', '4,6 g');
    assert.equal(app.field('add-portion').disabled, false);
    assert.equal(app.visible('portion-error'), false);
    assert.match(app.text('portion-total'), /41 kcal/);
    assert.equal(app.field('portion-presets').querySelector('button').getAttribute('aria-pressed'), 'true');
    app.submit('portion-form');
    assert.deepEqual(app.data, {...before, entries: [...before.entries,
        {id: 'added', date: today, meal: 'lunch', name: 'Huile d’olive · 4,6 g', kcal: 41}
    ]});
    const reopened = openApp(t, {data: app.data, detailed: false});
    reopened.field('meals').querySelectorAll('button')[2].click();
    reopened.field('recent-food-list').querySelector('button').click();
    assert.equal(reopened.field('grams').value, '4.6');
    assert.match(reopened.text('portion-total'), /41 kcal/);
    reopened.back();
    reopened.fill('food-search', 'banane');
    reopened.field('food-results').querySelector('button').click();
    for (const amount of ["1'250,5 g", '1’250,5 g', '1 250,5 g', '1\u00a0250,5 g', '1\u202f250,5 g']) {
        reopened.fill('grams', amount);
        assert.equal(reopened.field('add-portion').disabled, false, amount);
        assert.match(reopened.text('portion-total'), /1\s125 kcal/);
    }
    for (const amount of ['1,250 g', '1 25 g', '2’000,1 g']) {
        reopened.fill('grams', amount);
        assert.equal(reopened.field('add-portion').disabled, true, amount);
    }
});

test('consulte et corrige les aliments du repas sans descendre sous le catalogue', t => {
    const data = structuredClone(oldState);
    data.entries.push({id: 'yesterday', date: '2026-11-06', meal: 'lunch', name: 'Soupe', kcal: 130});
    const before = structuredClone(data);
    const app = openApp(t, {data, detailed: false});
    const openLunch = () => app.field('meals').querySelectorAll('button')[1].click();
    openLunch();

    const saved = app.field('meal-saved');
    const summary = app.field('meal-saved-title');
    assert.equal(saved.open, false);
    assert.equal(saved.nextElementSibling.className, 'catalog');
    assert.equal(summary.textContent, '1 aliment enregistré · 195 kcal');
    summary.click();
    assert.equal(saved.open, true);
    assert.match(app.text('meal-entries'), /Riz cuit/);
    assert.doesNotMatch(app.text('meal-entries'), /Soupe|Pomme/);
    assert.deepEqual(data, before);

    app.click('manual-open');
    app.fill('food', 'Riz cuit (150 g)');
    app.fill('kcal', '80');
    app.submit('entry-form');
    assert.equal(saved.open, true);
    assert.equal(summary.textContent, '2 aliments enregistrés · 275 kcal');
    const remove = app.field('meal-entries').querySelectorAll('button');
    assert.match(remove[0].getAttribute('aria-label'), /80 kcal/);
    assert.match(remove[1].getAttribute('aria-label'), /195 kcal/);
    remove[0].click();
    assert.equal(app.text('delete-name'), 'Riz cuit (150 g)');
    assert.match(app.text('delete-context'), /^80 kcal · Midi · .*7 novembre 2026$/);
    assert.equal(app.field('delete-dialog').getAttribute('aria-describedby'), 'delete-name delete-context');
    assert.equal(app.back(), true);
    assert.equal(summary.textContent, '2 aliments enregistrés · 275 kcal');
    remove[0].click();
    app.click('delete-confirm');
    assert.equal(app.document.activeElement, summary);
    assert.equal(summary.textContent, '1 aliment enregistré · 195 kcal');
    assert.deepEqual(data, before);

    app.click('back');
    app.click('previous');
    openLunch();
    assert.equal(saved.open, false);
    assert.equal(summary.textContent, '1 aliment enregistré · 130 kcal');
    summary.click();
    app.field('meal-entries').querySelector('button').click();
    assert.match(app.text('delete-context'), /^130 kcal · Midi · .*6 novembre 2026$/);
    app.click('delete-confirm');
    assert.equal(saved.hidden, true);
    assert.equal(saved.open, false);
    assert.equal(app.document.activeElement, app.field('meal-title'));
    assert.deepEqual(data, {...before, entries: before.entries.filter(entry => entry.id !== 'yesterday')});

    app.click('back');
    app.click('return-today');
    app.field('legacy-entries').querySelector('button').click();
    assert.match(app.text('delete-context'), /^52 kcal · samedi,? 7 novembre 2026$/);
});

test('reprend les aliments récents du repas avant ceux des autres repas', t => {
    const data = structuredClone(oldState);
    data.entries.push(
        {id: 'apple', date: today, meal: 'breakfast', name: 'Pomme · 150 g', kcal: 78},
        {id: 'pasta-breakfast', date: '2026-11-05', meal: 'breakfast', name: 'Pâtes sans œufs · sec · 50 g', kcal: 177},
        {id: 'pasta-dry', date: today, meal: 'lunch', name: 'Pâtes sans œufs · sec · 75 g', kcal: 265},
        {id: 'yogourt', date: today, meal: 'breakfast', name: 'Yogourt nature · 125 g', kcal: 83},
        {id: 'chocolate', date: today, meal: 'dinner', name: 'Chocolat noir · 20 g', kcal: 107},
        {id: 'pasta-cooked', date: '2026-11-06', meal: 'lunch', name: 'Pâtes sans œufs · cuit · 170,5 g', kcal: 269},
        {id: 'manual', date: today, meal: 'lunch', name: 'Pâtes sans œufs · sec · 80 g', kcal: 400}
    );
    const before = structuredClone(data);
    const app = openApp(t, {data, detailed: false});
    const recent = () => [...app.field('recent-food-list').querySelectorAll('button')];
    const names = () => recent().map(button => button.querySelector('span').firstChild.textContent);
    app.field('meals').querySelectorAll('button')[0].click();
    assert.deepEqual(names(), ['Yogourt nature', 'Pâtes sans œufs · sec', 'Pomme']);
    recent()[1].click();
    assert.equal(app.field('weighing-state').value, '0');
    assert.equal(app.field('grams').value, '50');
    app.field('portion-dialog').querySelector('.close').click();
    app.click('back');
    app.field('meals').querySelectorAll('button')[2].click();
    assert.equal(app.visible('recent-foods'), true);
    assert.deepEqual(names(), ['Chocolat noir', 'Pâtes sans œufs · cuit', 'Yogourt nature']);

    app.fill('food-search', 'pomme');
    assert.equal(app.visible('recent-foods'), false);
    assert.match(app.text('food-results'), /Pomme/);
    app.click('clear-food-search');
    recent()[1].click();
    assert.equal(app.field('weighing-state').value, '1');
    assert.equal(app.field('grams').value, '170.5');
    assert.match(app.text('portion-total'), /269 kcal/);
    app.field('portion-dialog').querySelector('.close').click();
    assert.deepEqual(data, before);

    recent()[1].click();
    app.fill('grams', '150');
    app.submit('portion-form');
    assert.deepEqual(data, {...before, entries: [...before.entries,
        {id: 'added', date: today, meal: 'dinner', name: 'Pâtes sans œufs · cuit · 150 g', kcal: 237}]});
    assert.equal(recent()[0].querySelector('small').textContent, '150 g');
    app.field('meal-entries').querySelector('button').click();
    app.click('delete-confirm');
    assert.deepEqual(data, before);
    assert.equal(recent()[1].querySelector('small').textContent, '170,5 g');
});

test('affiche et annonce le nombre de fiches trouvées sans écrire dans le journal', t => {
    const data = structuredClone(oldState);
    data.profile.weighIns = {[today]: 78.5};
    data.withingsWeights = {old: {day: '2026-11-01', kg: 78}};
    const before = structuredClone(data);
    const app = openApp(t, {data});
    app.field('meals').querySelectorAll('button')[1].click();

    const count = app.field('search-count');
    const results = app.field('food-results');
    assert.equal(count.getAttribute('role'), 'status');
    assert.equal(app.visible('search-count'), true);
    assert.equal(count.textContent, '');

    app.fill('food-search', 'pomme');
    assert.ok(results.childElementCount > 1);
    assert.equal(count.textContent, results.childElementCount + ' résultats');
    const announcement = count.firstChild;
    app.fill('food-search', 'POMME');
    assert.equal(count.firstChild, announcement);

    app.fill('food-search', 'pâtes sans œufs');
    assert.equal(results.childElementCount, 1);
    assert.equal(count.textContent, '1 résultat');
    results.firstElementChild.click();
    assert.equal(app.field('portion-dialog').open, true);
    app.back();
    assert.equal(count.textContent, '1 résultat');

    app.fill('food-search', 'sans correspondance');
    assert.equal(count.textContent, '0 résultat');
    assert.equal(app.visible('food-empty'), true);
    assert.equal(app.visible('manual-open'), true);

    app.back();
    assert.equal(count.textContent, '');
    assert.equal(app.visible('food-categories'), true);
    app.field('food-categories').querySelector('[data-group="starches"]').click();
    assert.equal(count.textContent, '');
    assert.deepEqual(data, before);
});

test('la portion place le focus sur le nom puis le rend à la fiche après annulation', t => {
    const data = structuredClone(oldState);
    data.entries.push({id: 'pasta', date: today, meal: 'lunch',
        name: 'Pâtes sans œufs · cuit · 170,5 g', kcal: 269});
    const before = structuredClone(data);
    const app = openApp(t, {data});
    app.field('meals').querySelector('button').click();
    const recent = app.field('recent-food-list').querySelector('button');
    app.field('food-search').focus();
    recent.click();
    assert.equal(app.document.activeElement, app.field('portion-title'));
    assert.match(app.text('portion-title'), /Pâtes/);
    assert.equal(app.field('portion-presets').getAttribute('role'), 'group');
    assert.equal(app.back(), true);
    assert.equal(app.document.activeElement, recent);

    app.fill('food-search', 'pomme');
    const result = app.field('food-results').querySelector('button');
    app.field('food-search').focus();
    result.click();
    assert.equal(app.document.activeElement, app.field('portion-title'));
    assert.equal(app.text('portion-title'), result.querySelector('span').firstChild.textContent);
    app.field('portion-dialog').querySelector('.close').click();
    assert.equal(app.document.activeElement, result);
    assert.equal(app.field('food-search').value, 'pomme');
    assert.deepEqual(data, before);
});

test('une longue liste d’aliments défile avec la page', t => {
    const app = openApp(t);
    app.field('meals').querySelector('button').click();
    app.field('food-categories').querySelector('[data-group="vegetables"]').click();
    const results = app.field('food-results');
    const assertPageScroll = () => {
        assert.ok(results.querySelectorAll('button').length > 10);
        const {overflow, overflowY, maxHeight} = app.document.defaultView.getComputedStyle(results);
        assert.doesNotMatch(overflow + ' ' + overflowY, /\b(auto|scroll)\b/);
        assert.ok(!maxHeight || maxHeight === 'none');
    };
    assertPageScroll();
    app.fill('food-search', 'cuit');
    assertPageScroll();
});

test('confirme un ajout du catalogue dans la fenêtre visible sans déplacer le focus', t => {
    const app = openApp(t);
    const window = app.document.defaultView;
    const feedback = app.field('catalog-feedback');
    const saveEntry = window.NutritionHost.addEntry;
    const timers = new Map();
    let nextTimer = 0;
    window.setTimeout = (callback, delay) => {
        assert.equal(delay, 5000);
        const id = ++nextTimer;
        timers.set(id, callback);
        return id;
    };
    window.clearTimeout = id => timers.delete(id);

    app.field('meals').querySelectorAll('button')[1].click();
    app.fill('food-search', 'pomme');
    const result = app.field('food-results').querySelector('button');
    result.focus();
    assert.equal(feedback.getAttribute('role'), 'status');
    assert.equal(feedback.hidden, false);
    assert.equal(window.getComputedStyle(feedback).position, 'fixed');
    assert.equal(window.getComputedStyle(feedback).pointerEvents, 'none');

    result.click();
    assert.equal(app.back(), true);
    assert.equal(app.data.entries.length, oldState.entries.length);
    assert.equal(feedback.textContent, '');

    result.click();
    window.NutritionHost.addEntry = () => false;
    app.submit('portion-form');
    assert.equal(app.field('portion-dialog').open, true);
    assert.match(app.text('portion-error'), /n’a pas été enregistré/);
    assert.equal(timers.size, 0);
    assert.equal(feedback.textContent, '');

    window.NutritionHost.addEntry = saveEntry;
    const focusBeforeSave = app.document.activeElement;
    app.submit('portion-form');
    assert.equal(app.field('portion-dialog').open, false);
    assert.ok([focusBeforeSave, result].includes(app.document.activeElement));
    assert.equal(app.field('food-search').value, 'pomme');
    assert.equal(app.data.entries.length, oldState.entries.length + 1);
    assert.deepEqual(app.data.entries.slice(0, -1), oldState.entries);
    assert.deepEqual(app.data.profile, oldState.profile);
    assert.equal(app.data.goal, oldState.goal);
    assert.equal(feedback.textContent, app.data.entries.at(-1).name + ' ajouté au repas.');
    assert.equal(timers.size, 1);

    result.click();
    assert.equal(feedback.textContent, '');
    assert.equal(timers.size, 0);
    app.submit('portion-form');
    const expire = timers.get(nextTimer);
    timers.delete(nextTimer);
    expire();
    assert.equal(feedback.textContent, '');

    result.click();
    app.submit('portion-form');
    app.click('back');
    assert.equal(feedback.textContent, '');
    assert.equal(timers.size, 0);
});

test('confirme une saisie manuelle seulement après son enregistrement', t => {
    const app = openApp(t);
    const host = app.document.defaultView.NutritionHost;
    const addEntry = host.addEntry;
    app.click('previous');
    const date = app.field('day').dateTime;
    app.field('meals').querySelectorAll('button')[2].click();
    app.click('manual-open');
    app.fill('food', '  Soupe maison  ');
    app.fill('kcal', '180');

    host.addEntry = () => false;
    app.submit('entry-form');
    assert.equal(app.text('catalog-feedback'), '');
    assert.equal(app.field('food').value, '  Soupe maison  ');
    assert.equal(app.field('kcal').value, '180');
    assert.equal(app.data.entries.length, oldState.entries.length);
    assert.equal(app.visible('entry-error'), true);

    host.addEntry = (day, meal, name, kcal) => addEntry(day, meal, name.trim(), kcal);
    app.submit('entry-form');
    assert.equal(app.text('catalog-feedback'), 'Ajouté au repas · Soupe maison · 180 kcal');
    assert.equal(app.visible('entry-error'), false);
    assert.equal(app.field('food').value, '');
    assert.equal(app.field('kcal').value, '');
    assert.deepEqual(app.data, {
        ...oldState,
        entries: [...oldState.entries, {id: 'added', date, meal: 'dinner', name: 'Soupe maison', kcal: 180}]
    });
    assert.match(app.text('meal-entries'), /Soupe maison/);
});
