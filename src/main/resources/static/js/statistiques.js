const COULEUR_ACTIFS = '#fd7e14';
const COULEUR_RESOLUS = '#198754';
const COULEUR_PRINCIPALE = '#0d6efd';
const PALETTE_TYPES = ['#0d6efd', '#fd7e14', '#198754', '#6f42c1', '#dc3545', '#20c997'];
const NOTE_MAXIMALE = 5;

function creerGraphique(idCanvas, type, libelles, jeuxDeDonnees, options) {
    new Chart(document.getElementById(idCanvas), {
        type: type,
        data: { labels: libelles, datasets: jeuxDeDonnees },
        options: options
    });
}

function optionsAxeEntier(empile) {
    return {
        scales: {
            x: { stacked: empile },
            y: { stacked: empile, beginAtZero: true, ticks: { precision: 0 } }
        }
    };
}

function optionsBarresHorizontales(maximum) {
    return {
        indexAxis: 'y',
        plugins: { legend: { display: false } },
        scales: { x: { beginAtZero: true, max: maximum } }
    };
}

creerGraphique('graphiqueOuvertures', 'line', statistiques.ouverturesParJour.libelles, [{
    label: 'Incidents ouverts',
    data: statistiques.ouverturesParJour.valeurs,
    borderColor: COULEUR_PRINCIPALE,
    backgroundColor: COULEUR_PRINCIPALE
}], { plugins: { legend: { display: false } }, ...optionsAxeEntier(false) });

creerGraphique('graphiqueStations', 'bar', statistiques.stations, [
    { label: 'Actifs', data: statistiques.actifsParStation, backgroundColor: COULEUR_ACTIFS },
    { label: 'Résolus', data: statistiques.resolusParStation, backgroundColor: COULEUR_RESOLUS }
], optionsAxeEntier(true));

creerGraphique('graphiqueTypes', 'doughnut', statistiques.incidentsParType.libelles, [{
    data: statistiques.incidentsParType.valeurs,
    backgroundColor: PALETTE_TYPES
}], { aspectRatio: 2 });

creerGraphique('graphiqueNotes', 'bar', statistiques.notesParPrestataire.libelles, [{
    label: 'Note moyenne',
    data: statistiques.notesParPrestataire.valeurs,
    backgroundColor: COULEUR_RESOLUS
}], optionsBarresHorizontales(NOTE_MAXIMALE));

creerGraphique('graphiqueDelais', 'bar', statistiques.delaisResolutionParPrestataire.libelles, [{
    label: 'Délai moyen (heures)',
    data: statistiques.delaisResolutionParPrestataire.valeurs,
    backgroundColor: COULEUR_PRINCIPALE
}], optionsBarresHorizontales());
