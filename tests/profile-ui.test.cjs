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
    readWithings, readSteps, profileBackup} = {}) {
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
            window.HTMLDialogElement.prototype.showModal = function () { this.open = true; };
            window.HTMLDialogElement.prototype.close = function () { this.open = false; };
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
                }
            };
            if (profileBackup) window.NutritionHost.profileBackup = profileBackup;
            if (readSteps) {
                window.NutritionHost.readSteps = (day, interactive) => readSteps(day, interactive, result => {
                    window.dispatchEvent(new window.CustomEvent('nutrition-steps', {detail: {day, ...result}}));
                });
            }
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

test('ouvre la sauvegarde et la restauration du profil depuis les paramètres', t => {
    const actions = [];
    const app = openApp(t, {profileBackup: restore => actions.push(restore)});
    app.click('settings-button');
    assert.equal(app.visible('export-profile'), true);
    app.click('export-profile');
    app.click('restore-profile');
    assert.deepEqual(actions, [false, true]);
});

test('restaure un profil dès le premier lancement et ouvre le journal', t => {
    const data = {...structuredClone(oldState), profile: null, goal: 2000};
    const actions = [];
    const app = openApp(t, {data, profileBackup: restore => actions.push(restore)});
    assert.equal(app.visible('restore-first-profile'), true);
    app.click('restore-first-profile');
    assert.deepEqual(actions, [true]);

    data.profile = structuredClone(oldState.profile);
    data.goal = oldState.goal;
    app.document.defaultView.dispatchEvent(new app.document.defaultView.Event('nutrition-profile-restored'));
    assert.equal(app.visible('today'), true);
    assert.match(app.text('goal-button'), /2\s175 kcal/);
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
    assert.equal(app.field('food-categories').querySelectorAll('button').length, 8);
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


test('lit les pas avec autorisation explicite et sépare les deux sources', t => {
    const calls = [];
    const app = openApp(t, {readSteps: (day, interactive, reply) => {
        calls.push({day, interactive});
        reply(interactive ? {status: 'done', fit: 5432, nolatr: null} : {status: 'denied'});
    }});
    assert.deepEqual(calls, [{day: today, interactive: false}]);
    assert.match(app.text('steps-status'), /Autorise la lecture/);
    app.click('steps-refresh');
    assert.deepEqual(calls.at(-1), {day: today, interactive: true});
    assert.match(app.text('fit-steps'), /^5\s432 pas$/);
    assert.equal(app.text('nolatr-steps'), 'Aucun pas transmis');
    assert.equal(app.text('total'), '247 kcal');
});

test('masque le résultat périmé en changeant de jour et distingue zéro des données absentes', t => {
    const replies = [];
    const app = openApp(t, {readSteps: (day, interactive, reply) => replies.push({day, interactive, reply})});
    app.click('previous');
    assert.equal(replies[1].day, '2026-11-06');
    replies[0].reply({status: 'done', fit: 123, nolatr: 456});
    assert.equal(app.visible('steps-counts'), false);
    replies[1].reply({status: 'done', fit: 0, nolatr: 1234});
    assert.equal(app.text('fit-steps'), '0 pas');
    assert.match(app.text('nolatr-steps'), /^1\s234 pas$/);
});
