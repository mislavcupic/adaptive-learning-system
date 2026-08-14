-- =====================================================================
-- Testni podaci za provjeru ANCOVA analize
-- AdaptiveLearningSystem / diplomski rad
--
-- 24 ispitanika (12 CONTROL, 12 EXPERIMENTAL), pretest i posttest po 20 bodova.
-- Skupine krecu s priblizno istog pretesta; eksperimentalna napreduje vise.
-- Ocekivani Hedges g je oko 1,1-1,3 (velik, ali vjerodostojan ucinak).
--
-- Sve je oznaceno prefiksom [TEST] i domenom @example.test radi lakog brisanja.
-- Skripta se izvrsava u jednoj transakciji: ako nesto pukne, nista se ne upise.
-- =====================================================================

DO $$
DECLARE
v_course_id  UUID;
    v_teacher_id UUID;
    v_pre_id     UUID;
    v_post_id    UUID;
    v_student_id UUID;
    -- BCrypt hash za lozinku "Test1234"
    v_pass       TEXT := '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';
    r            RECORD;
BEGIN
    -- Preuzmi prvi kolegij i prvog nastavnika iz baze
SELECT id INTO v_course_id FROM courses ORDER BY created_at LIMIT 1;
IF v_course_id IS NULL THEN
        RAISE EXCEPTION 'Nema nijednog kolegija u bazi. Kreiraj kolegij prije pokretanja skripte.';
END IF;

SELECT id INTO v_teacher_id FROM users
WHERE role IN ('TEACHER', 'ADMIN')
ORDER BY created_at LIMIT 1;
IF v_teacher_id IS NULL THEN
        RAISE EXCEPTION 'Nema nastavnika ni administratora u bazi.';
END IF;

    -- -----------------------------------------------------------------
    -- Testovi
    -- -----------------------------------------------------------------
    v_pre_id  := gen_random_uuid();
    v_post_id := gen_random_uuid();

INSERT INTO assessments (id, created_at, updated_at, title, description,
                         assessment_type, course_id, created_by,
                         time_limit_minutes, passing_score, is_active)
VALUES
    (v_pre_id,
     NOW() - INTERVAL '60 days', NOW() - INTERVAL '60 days',
     '[TEST] Predtest - Osnove programiranja',
     'Generirano za provjeru statisticke analize',
     'PRETEST', v_course_id, v_teacher_id, 30, 50, TRUE),
    (v_post_id,
     NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days',
     '[TEST] Posttest - Osnove programiranja',
     'Generirano za provjeru statisticke analize',
     'POSTTEST', v_course_id, v_teacher_id, 30, 50, TRUE);

-- -----------------------------------------------------------------
-- Ispitanici i njihovi pokusaji
-- -----------------------------------------------------------------
FOR r IN
SELECT * FROM (VALUES
                   -- grupa,           br, pretest, posttest
                   ('CONTROL',       1,  9, 11),
                   ('CONTROL',       2, 11, 14),
                   ('CONTROL',       3,  8,  9),
                   ('CONTROL',       4, 13, 15),
                   ('CONTROL',       5, 10, 13),
                   ('CONTROL',       6, 12, 12),
                   ('CONTROL',       7,  7, 10),
                   ('CONTROL',       8, 14, 16),
                   ('CONTROL',       9, 10, 11),
                   ('CONTROL',      10, 11, 14),
                   ('CONTROL',      11,  9, 10),
                   ('CONTROL',      12, 12, 13),
                   ('EXPERIMENTAL',  1, 10, 14),
                   ('EXPERIMENTAL',  2, 12, 16),
                   ('EXPERIMENTAL',  3,  9, 12),
                   ('EXPERIMENTAL',  4, 13, 19),
                   ('EXPERIMENTAL',  5, 11, 15),
                   ('EXPERIMENTAL',  6, 10, 14),
                   ('EXPERIMENTAL',  7,  8, 11),
                   ('EXPERIMENTAL',  8, 14, 18),
                   ('EXPERIMENTAL',  9, 11, 16),
                   ('EXPERIMENTAL', 10,  9, 13),
                   ('EXPERIMENTAL', 11, 12, 17),
                   ('EXPERIMENTAL', 12, 10, 14)
              ) AS t(grp, idx, pre, post)
    LOOP
        v_student_id := gen_random_uuid();

INSERT INTO users (id, created_at, updated_at, email, password,
                   first_name, last_name, role, is_active,
                   research_group, group_type)
VALUES (v_student_id,
        NOW() - INTERVAL '70 days', NOW(),
        'test.' || LOWER(r.grp) || '.' || r.idx || '@example.test',
        v_pass,
        'Ispitanik',
        CASE WHEN r.grp = 'CONTROL' THEN 'Kontrolna ' ELSE 'Eksperimentalna ' END || r.idx,
        'STUDENT', TRUE,
        r.grp, r.grp);

INSERT INTO assessment_attempts (id, created_at, updated_at, student_id,
                                 assessment_id, started_at, completed_at,
                                 score, max_score, is_completed)
VALUES
    (gen_random_uuid(),
     NOW() - INTERVAL '60 days', NOW() - INTERVAL '60 days',
     v_student_id, v_pre_id,
     NOW() - INTERVAL '60 days',
     NOW() - INTERVAL '60 days' + INTERVAL '25 minutes',
     r.pre, 20, TRUE),
    (gen_random_uuid(),
     NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days',
     v_student_id, v_post_id,
     NOW() - INTERVAL '5 days',
     NOW() - INTERVAL '5 days' + INTERVAL '22 minutes',
     r.post, 20, TRUE);
END LOOP;

    RAISE NOTICE 'Uneseno 24 ispitanika i 48 pokusaja.';
END $$;


-- =====================================================================
-- PROVJERA
-- =====================================================================

SELECT u.research_group AS skupina,
       COUNT(*)                                        AS n,
       ROUND(AVG(pre.score  * 100.0 / pre.max_score), 2)  AS pretest_posto,
       ROUND(AVG(post.score * 100.0 / post.max_score), 2) AS posttest_posto
FROM users u
         JOIN assessment_attempts pre  ON pre.student_id  = u.id
         JOIN assessments         a1   ON a1.id = pre.assessment_id
    AND a1.assessment_type = 'PRETEST'
         JOIN assessment_attempts post ON post.student_id = u.id
         JOIN assessments         a2   ON a2.id = post.assessment_id
    AND a2.assessment_type = 'POSTTEST'
WHERE u.email LIKE 'test.%@example.test'
GROUP BY u.research_group
ORDER BY u.research_group;


-- =====================================================================
-- BRISANJE TESTNIH PODATAKA
-- Odkomentiraj i pokreni kad zavrsis s provjerom.
-- =====================================================================

-- DELETE FROM assessment_attempts
--  WHERE student_id IN (SELECT id FROM users WHERE email LIKE 'test.%@example.test');
-- DELETE FROM users     WHERE email LIKE 'test.%@example.test';
-- DELETE FROM assessments WHERE title LIKE '[TEST]%';
