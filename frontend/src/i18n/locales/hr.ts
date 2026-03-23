// ============================================
// HRVATSKI PRIJEVODI
// ============================================

const hr = {
    // ============ COMMON ============
    common: {
        appName: 'Adaptivno Učenje',
        loading: 'Učitavanje...',
        save: 'Spremi',
        cancel: 'Odustani',
        delete: 'Obriši',
        edit: 'Uredi',
        create: 'Kreiraj',
        search: 'Pretraži',
        filter: 'Filtriraj',
        export: 'Izvezi',
        import: 'Uvezi',
        back: 'Natrag',
        next: 'Dalje',
        previous: 'Prethodno',
        submit: 'Predaj',
        confirm: 'Potvrdi',
        close: 'Zatvori',
        yes: 'Da',
        no: 'Ne',
        all: 'Sve',
        none: 'Ništa',
        select: 'Odaberi',
        noData: 'Nema podataka',
        actions: 'Akcije',
        details: 'Detalji',
        status: 'Status',
        date: 'Datum',
        time: 'Vrijeme',
        description: 'Opis',
        name: 'Naziv',
        type: 'Tip',
        success: 'Uspješno',
        error: 'Greška',
        warning: 'Upozorenje',
        info: 'Informacija',
    },

    // ============ AUTH ============
    auth: {
        login: 'Prijava',
        logout: 'Odjava',
        email: 'Email',
        password: 'Lozinka',
        forgotPassword: 'Zaboravljena lozinka?',
        rememberMe: 'Zapamti me',
        loginButton: 'Prijavi se',
        loggingIn: 'Prijava u tijeku...',
        loginSuccess: 'Uspješna prijava!',
        loginError: 'Pogrešan email ili lozinka',
        logoutSuccess: 'Uspješna odjava',
        sessionExpired: 'Sesija je istekla, prijavite se ponovo',
        unauthorized: 'Nemate pristup ovoj stranici',
    },

    // ============ ROLES ============
    roles: {
        admin: 'Administrator',
        teacher: 'Nastavnik',
        student: 'Učenik',
    },

    // ============ GROUPS ============
    groups: {
        experimental: 'Eksperimentalna',
        control: 'Kontrolna',
        experimentalShort: 'EXP',
        controlShort: 'CTRL',
    },

    // ============ NAVIGATION ============
    nav: {
        dashboard: 'Nadzorna ploča',
        courses: 'Kolegiji',
        classes: 'Razredi',
        tasks: 'Zadaci',
        exams: 'Ispiti',
        students: 'Učenici',
        teachers: 'Nastavnici',
        progress: 'Napredak',
        analytics: 'Analitika',
        surveys: 'Upitnici',
        settings: 'Postavke',
        profile: 'Profil',
        help: 'Pomoć',
    },

    // ============ DASHBOARD ============
    dashboard: {
        welcome: 'Dobrodošli, {{name}}!',
        welcomeBack: 'Dobrodošli natrag, {{name}}!',
        lastLogin: 'Zadnja prijava: {{date}}',
        quickActions: 'Brze akcije',
        recentActivity: 'Nedavna aktivnost',
        upcomingTasks: 'Nadolazeći zadaci',
        noUpcomingTasks: 'Nema nadolazećih zadataka',

        // Student specific
        continuelearning: 'Nastavi učiti',
        practiceRecommendation: 'Preporučujemo vježbu',
        daysSinceLastPractice: 'Dana od zadnje vježbe: {{days}}',
        skillsToFocus: 'Vještine za fokusiranje',

        // Teacher specific
        pendingSubmissions: 'Predaje na čekanju',
        classOverview: 'Pregled razreda',
        commonIssues: 'Česti problemi',
    },

    // ============ CLASSES ============
    classes: {
        title: 'Razredi',
        addClass: 'Dodaj razred',
        editClass: 'Uredi razred',
        deleteClass: 'Obriši razred',
        className: 'Naziv razreda',
        schoolYear: 'Školska godina',
        studentCount: 'Broj učenika',
        experimentalCount: 'Eksperimentalna grupa',
        controlCount: 'Kontrolna grupa',
        importStudents: 'Uvezi učenike',
        importFromCsv: 'Uvezi iz CSV-a (e-Dnevnik)',
        randomizeGroups: 'Nasumično rasporedi grupe',
        randomizeConfirm: 'Jeste li sigurni da želite nasumično rasporediti učenike u eksperimentalnu i kontrolnu grupu?',
        randomizeSuccess: 'Učenici su uspješno raspoređeni',
        viewStudents: 'Pregled učenika',
        noClasses: 'Nema kreiranih razreda',
    },

    // ============ COURSES ============
    courses: {
        title: 'Kolegiji',
        addCourse: 'Dodaj kolegij',
        editCourse: 'Uredi kolegij',
        deleteCourse: 'Obriši kolegij',
        courseName: 'Naziv kolegija',
        courseCode: 'Šifra kolegija',
        language: 'Programski jezik',
        outcomes: 'Ishodi učenja',
        addOutcome: 'Dodaj ishod',
        editOutcome: 'Uredi ishod',
        outcomeCode: 'Šifra ishoda',
        outcomeDescription: 'Opis ishoda',
        skills: 'Vještine',
        noCourses: 'Nema kreiranih kolegija',
        noOutcomes: 'Nema definiranih ishoda',
    },

    // ============ TASKS ============
    tasks: {
        title: 'Zadaci',
        addTask: 'Dodaj zadatak',
        editTask: 'Uredi zadatak',
        deleteTask: 'Obriši zadatak',
        taskTitle: 'Naslov zadatka',
        taskDescription: 'Opis zadatka',
        taskType: 'Tip zadatka',
        difficulty: 'Težina',
        timeLimit: 'Vremensko ograničenje (min)',
        availableFrom: 'Dostupno od',
        availableTo: 'Dostupno do',
        testCases: 'Test primjeri',
        addTestCase: 'Dodaj test primjer',
        input: 'Ulaz',
        expectedOutput: 'Očekivani izlaz',
        hiddenTest: 'Skriveni test',
        gradingCriteria: 'Kriteriji ocjenjivanja',
        addCriterion: 'Dodaj kriterij',
        criterion: 'Kriterij',
        maxPoints: 'Maksimalni bodovi',
        weight: 'Težina',
        starterCode: 'Početni kod',
        expectedSolution: 'Očekivano rješenje',
        generateTask: 'Generiraj zadatak (AI)',
        noTasks: 'Nema kreiranih zadataka',

        // Task types
        types: {
            practice: 'Vježba',
            homework: 'Domaća zadaća',
            exam: 'Ispit',
        },

        // Difficulty
        difficulties: {
            1: 'Vrlo lako',
            2: 'Lako',
            3: 'Srednje',
            4: 'Teško',
            5: 'Vrlo teško',
        },
    },

    // ============ EXAMS ============
    exams: {
        title: 'Ispiti',
        addExam: 'Dodaj ispit',
        editExam: 'Uredi ispit',
        deleteExam: 'Obriši ispit',
        examTitle: 'Naslov ispita',
        duration: 'Trajanje (min)',
        totalPoints: 'Ukupno bodova',
        selectTasks: 'Odaberi zadatke',
        assignToClasses: 'Dodijeli razredima',
        noExams: 'Nema kreiranih ispita',
        startExam: 'Započni ispit',
        endExam: 'Završi ispit',
        timeRemaining: 'Preostalo vrijeme',
        examInProgress: 'Ispit u tijeku',
        examCompleted: 'Ispit završen',
    },

    // ============ SUBMISSIONS ============
    submissions: {
        title: 'Predaje',
        submitCode: 'Predaj kod',
        submitting: 'Predaja u tijeku...',
        submitSuccess: 'Kod uspješno predan!',
        submitError: 'Greška pri predaji koda',
        viewSubmission: 'Pregled predaje',
        mySubmissions: 'Moje predaje',
        allSubmissions: 'Sve predaje',
        compileSuccess: 'Kompilacija uspješna',
        compileError: 'Greška pri kompilaciji',
        compileErrors: 'Greške kompilacije',
        testResults: 'Rezultati testova',
        testsPassed: 'Prolaznih testova',
        memoryAnalysis: 'Analiza memorije',
        memoryLeaks: 'Pronađeni memory leakovi',
        noMemoryLeaks: 'Nema memory leakova',
        aiFeedback: 'AI povratna informacija',
        teacherFeedback: 'Povratna informacija nastavnika',
        addFeedback: 'Dodaj povratnu informaciju',
        score: 'Ocjena',
        noSubmissions: 'Nema predaja',
    },

    // ============ EDITOR ============
    editor: {
        title: 'Editor koda',
        run: 'Pokreni',
        running: 'Pokretanje...',
        reset: 'Resetiraj',
        output: 'Izlaz',
        input: 'Ulaz',
        console: 'Konzola',
        clearConsole: 'Očisti konzolu',
        fontSize: 'Veličina fonta',
        theme: 'Tema',
        darkTheme: 'Tamna',
        lightTheme: 'Svijetla',
    },

    // ============ PROGRESS & MASTERY ============
    progress: {
        title: 'Napredak',
        myProgress: 'Moj napredak',
        studentProgress: 'Napredak učenika',
        classProgress: 'Napredak razreda',
        skillMastery: 'Razina svladavanja vještina',
        masteryLevel: 'Razina svladavanja',
        totalAttempts: 'Ukupno pokušaja',
        correctAttempts: 'Točnih pokušaja',
        lastPracticed: 'Zadnja vježba',
        trend: 'Trend',

        // Mastery levels
        mastery: {
            notStarted: 'Nije započeto',
            beginner: 'Početnik',
            intermediate: 'Srednji',
            advanced: 'Napredni',
            mastered: 'Savladano',
        },

        // Trends
        trends: {
            improving: 'Napreduje',
            stable: 'Stabilno',
            declining: 'Opada',
        },
    },

    // ============ ANALYTICS ============
    analytics: {
        title: 'Analitika',
        classStatistics: 'Statistika razreda',
        groupComparison: 'Usporedba grupa',
        weeklyProgress: 'Tjedni napredak',
        commonIssues: 'Česti problemi',
        affectedStudents: 'Pogođenih učenika',
        occurrences: 'Pojavljivanja',
        avgMastery: 'Prosječna razina svladavanja',
        avgScore: 'Prosječna ocjena',
        submissionCount: 'Broj predaja',
        completionRate: 'Stopa završavanja',
        experimentalGroup: 'Eksperimentalna grupa',
        controlGroup: 'Kontrolna grupa',
        difference: 'Razlika',
        significanceLevel: 'Razina značajnosti',
        strengths: 'Snage',
        weaknesses: 'Slabosti',
        recommendations: 'Preporuke',
    },

    // ============ NOTES ============
    notes: {
        title: 'Bilješke',
        teacherNotes: 'Bilješke nastavnika',
        aiNotes: 'AI bilješke',
        addNote: 'Dodaj bilješku',
        editNote: 'Uredi bilješku',
        deleteNote: 'Obriši bilješku',
        importNotes: 'Uvezi bilješke',
        importFromCsv: 'Uvezi iz CSV-a (e-Dnevnik)',
        noteContent: 'Sadržaj bilješke',
        source: 'Izvor',
        sources: {
            aiGenerated: 'AI generirano',
            teacherManual: 'Nastavnik',
            csvImport: 'CSV uvoz',
        },
        noNotes: 'Nema bilješki',
    },

    // ============ SURVEYS ============
    surveys: {
        title: 'Upitnici',
        addSurvey: 'Kreiraj upitnik',
        editSurvey: 'Uredi upitnik',
        deleteSurvey: 'Obriši upitnik',
        surveyTitle: 'Naslov upitnika',
        targetAudience: 'Ciljna skupina',
        questions: 'Pitanja',
        addQuestion: 'Dodaj pitanje',
        questionText: 'Tekst pitanja',
        questionType: 'Tip pitanja',
        options: 'Opcije',
        active: 'Aktivan',
        inactive: 'Neaktivan',
        responses: 'Odgovori',
        viewResponses: 'Pregled odgovora',
        submitSurvey: 'Pošalji odgovore',
        surveyCompleted: 'Upitnik je uspješno ispunjen!',
        noSurveys: 'Nema aktivnih upitnika',

        // Question types
        questionTypes: {
            likert5: 'Likert skala (1-5)',
            likert7: 'Likert skala (1-7)',
            text: 'Tekstualni odgovor',
            multipleChoice: 'Višestruki izbor',
        },

        // Likert labels
        likert5: {
            1: 'Uopće se ne slažem',
            2: 'Ne slažem se',
            3: 'Niti se slažem niti ne slažem',
            4: 'Slažem se',
            5: 'U potpunosti se slažem',
        },
    },

    // ============ USERS ============
    users: {
        title: 'Korisnici',
        addUser: 'Dodaj korisnika',
        editUser: 'Uredi korisnika',
        deleteUser: 'Obriši korisnika',
        firstName: 'Ime',
        lastName: 'Prezime',
        role: 'Uloga',
        class: 'Razred',
        group: 'Grupa',
        enrolledCourses: 'Upisani kolegiji',
        createdAt: 'Datum kreiranja',
        noUsers: 'Nema korisnika',
    },

    // ============ CSV IMPORT ============
    csv: {
        importTitle: 'Uvoz iz CSV-a',
        selectFile: 'Odaberi datoteku',
        dragDrop: 'Povuci i ispusti datoteku ovdje',
        or: 'ili',
        browse: 'Pregledaj',
        importing: 'Uvoz u tijeku...',
        importSuccess: 'Uspješno uvezeno {{count}} zapisa',
        importError: 'Greška pri uvozu',
        totalRows: 'Ukupno redaka',
        importedRows: 'Uvezenih redaka',
        errors: 'Greške',
        row: 'Redak',
        field: 'Polje',
        message: 'Poruka',
    },

    // ============ LANGUAGES ============
    languages: {
        title: 'Jezik',
        hr: 'Hrvatski',
        en: 'English',
        c: 'C',
        csharp: 'C#',
    },

    // ============ ERRORS ============
    errors: {
        general: 'Došlo je do greške',
        notFound: 'Stranica nije pronađena',
        serverError: 'Greška na serveru',
        networkError: 'Greška u komunikaciji s serverom',
        validationError: 'Provjerite unesene podatke',
        required: 'Ovo polje je obavezno',
        invalidEmail: 'Unesite ispravnu email adresu',
        minLength: 'Minimalna duljina je {{min}} znakova',
        maxLength: 'Maksimalna duljina je {{max}} znakova',
        invalidFormat: 'Neispravan format',
        fileTooLarge: 'Datoteka je prevelika',
        invalidFileType: 'Nepodržani tip datoteke',
    },

    // ============ CONFIRMATIONS ============
    confirmations: {
        delete: 'Jeste li sigurni da želite obrisati?',
        deleteWarning: 'Ova radnja se ne može poništiti.',
        logout: 'Jeste li sigurni da se želite odjaviti?',
        unsavedChanges: 'Imate nespremljene promjene. Želite li napustiti stranicu?',
        randomizeGroups: 'Ova radnja će nasumično rasporediti učenike u grupe. Želite li nastaviti?',
    },
};

export default hr;