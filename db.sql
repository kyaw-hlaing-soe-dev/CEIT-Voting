-- KTU Voting App database bootstrap (PostgreSQL)
BEGIN;

-- 1) Voters capture per-device meta used for fraud prevention
CREATE TABLE IF NOT EXISTS voters (
    id             BIGSERIAL PRIMARY KEY,
    pin            VARCHAR(5)   NOT NULL,
    device_id      VARCHAR(255) NOT NULL,
    user_agent     VARCHAR(512),
    ip_address     VARCHAR(45),
    fingerprint    VARCHAR(512),
    hardware_hash  VARCHAR(128),
    screen_info    VARCHAR(100),
    has_voted      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    voted_at       TIMESTAMP,
    external_id    VARCHAR(255),
    CONSTRAINT uk_device_id UNIQUE (device_id),
    CONSTRAINT uk_external_id UNIQUE (external_id),
    CONSTRAINT chk_pin_length CHECK (char_length(pin) = 5)
);
CREATE INDEX IF NOT EXISTS idx_pin           ON voters(pin);
CREATE INDEX IF NOT EXISTS idx_has_voted     ON voters(has_voted);
CREATE INDEX IF NOT EXISTS idx_device_id     ON voters(device_id);
CREATE INDEX IF NOT EXISTS idx_ip_address    ON voters(ip_address);
CREATE INDEX IF NOT EXISTS idx_fingerprint   ON voters(fingerprint);
CREATE INDEX IF NOT EXISTS idx_hardware_hash ON voters(hardware_hash);

-- 2) Candidates (category + candidate_number unique pair)
CREATE TABLE IF NOT EXISTS candidates (
    id                BIGSERIAL PRIMARY KEY,
    category          VARCHAR(20)  NOT NULL,
    candidate_number  INTEGER      NOT NULL,
    name              VARCHAR(100) NOT NULL,
    department        VARCHAR(100),
    image_url         VARCHAR(500),
    vote_count        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_category_candidate UNIQUE (category, candidate_number),
    CONSTRAINT chk_candidate_number CHECK (candidate_number BETWEEN 1 AND 99),
    CONSTRAINT chk_category CHECK (category IN ('KING','QUEEN','PRINCE','PRINCESS','COUPLE'))
);
CREATE INDEX IF NOT EXISTS idx_category_number ON candidates(category, candidate_number);

-- 3) Votes (one vote per voter per category) + denormalized candidate_number for analytics
CREATE TABLE IF NOT EXISTS votes (
    id               BIGSERIAL PRIMARY KEY,
    voter_id         BIGINT      NOT NULL REFERENCES voters(id)    ON DELETE RESTRICT,
    candidate_id     BIGINT      NOT NULL REFERENCES candidates(id) ON DELETE RESTRICT,
    category         VARCHAR(20) NOT NULL,
    candidate_number INTEGER,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_voter_category UNIQUE (voter_id, category),
    CONSTRAINT chk_vote_category CHECK (category IN ('KING','QUEEN','PRINCE','PRINCESS','COUPLE'))
);
CREATE INDEX IF NOT EXISTS idx_voter_id       ON votes(voter_id);
CREATE INDEX IF NOT EXISTS idx_candidate_id   ON votes(candidate_id);
CREATE INDEX IF NOT EXISTS idx_category       ON votes(category);
CREATE INDEX IF NOT EXISTS idx_voter_category ON votes(voter_id, category);

-- 4) Admin audit trail for privileged operations
CREATE TABLE IF NOT EXISTS admin_action_audit (
    id           BIGSERIAL PRIMARY KEY,
    action       VARCHAR(100)  NOT NULL,
    details      VARCHAR(1000),
    performed_by VARCHAR(200),
    performed_at TIMESTAMP      NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_admin_action_performed_at ON admin_action_audit(performed_at);

-- 5) Triggers to keep denormalized columns and integrity in sync
CREATE OR REPLACE FUNCTION fn_vote_before_insert() RETURNS TRIGGER AS $$
DECLARE
    candidate_record candidates%ROWTYPE;
BEGIN
    SELECT * INTO candidate_record FROM candidates WHERE id = NEW.candidate_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Candidate % does not exist', NEW.candidate_id;
    END IF;
    IF NEW.category IS DISTINCT FROM candidate_record.category THEN
        RAISE EXCEPTION 'Vote category % does not match candidate category %', NEW.category, candidate_record.category;
    END IF;
    NEW.candidate_number := candidate_record.candidate_number;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_vote_after_insert() RETURNS TRIGGER AS $$
BEGIN
    UPDATE candidates SET vote_count = vote_count + 1 WHERE id = NEW.candidate_id;
    UPDATE voters
       SET has_voted = TRUE,
           voted_at  = COALESCE(voted_at, NEW.created_at)
     WHERE id = NEW.voter_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_vote_after_delete() RETURNS TRIGGER AS $$
BEGIN
    UPDATE candidates SET vote_count = GREATEST(0, vote_count - 1) WHERE id = OLD.candidate_id;
    UPDATE voters
       SET has_voted = EXISTS (SELECT 1 FROM votes WHERE voter_id = OLD.voter_id),
           voted_at  = (SELECT MAX(created_at) FROM votes WHERE voter_id = OLD.voter_id)
     WHERE id = OLD.voter_id;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_vote_before_insert ON votes;
CREATE TRIGGER trg_vote_before_insert
    BEFORE INSERT ON votes
    FOR EACH ROW
    EXECUTE FUNCTION fn_vote_before_insert();

DROP TRIGGER IF EXISTS trg_vote_after_insert ON votes;
CREATE TRIGGER trg_vote_after_insert
    AFTER INSERT ON votes
    FOR EACH ROW
    EXECUTE FUNCTION fn_vote_after_insert();

DROP TRIGGER IF EXISTS trg_vote_after_delete ON votes;
CREATE TRIGGER trg_vote_after_delete
    AFTER DELETE ON votes
    FOR EACH ROW
    EXECUTE FUNCTION fn_vote_after_delete();

-- 6) Candidate seeds (idempotent upsert per category+number)
INSERT INTO candidates (category, candidate_number, name, department, image_url)
VALUES
    ('KING',      1, 'Candidate King 1',      'Computer Science',        '/images/king1.jpg'),
    ('KING',      2, 'Candidate King 2',      'Electronics',             '/images/king2.jpg'),
    ('KING',      3, 'Candidate King 3',      'Mechanical',              '/images/king3.jpg'),
    ('KING',      4, 'Candidate King 4',      'Civil Engineering',       '/images/king4.jpg'),
    ('KING',      5, 'Candidate King 5',      'Architecture',            '/images/king5.jpg'),
    ('KING',      6, 'Candidate King 6',      'Chemical Engineering',    '/images/king6.jpg'),
    ('KING',      7, 'Candidate King 7',      'Information Technology',  '/images/king7.jpg'),
    ('KING',      8, 'Candidate King 8',      'Electrical Engineering',  '/images/king8.jpg'),
    ('KING',      9, 'Candidate King 9',      'Mining Engineering',      '/images/king9.jpg'),
    ('QUEEN',     1, 'Candidate Queen 1',     'Computer Science',        '/images/queen1.jpg'),
    ('QUEEN',     2, 'Candidate Queen 2',     'Electronics',             '/images/queen2.jpg'),
    ('QUEEN',     3, 'Candidate Queen 3',     'Mechanical',              '/images/queen3.jpg'),
    ('QUEEN',     4, 'Candidate Queen 4',     'Civil Engineering',       '/images/queen4.jpg'),
    ('QUEEN',     5, 'Candidate Queen 5',     'Architecture',            '/images/queen5.jpg'),
    ('QUEEN',     6, 'Candidate Queen 6',     'Chemical Engineering',    '/images/queen6.jpg'),
    ('QUEEN',     7, 'Candidate Queen 7',     'Information Technology',  '/images/queen7.jpg'),
    ('QUEEN',     8, 'Candidate Queen 8',     'Electrical Engineering',  '/images/queen8.jpg'),
    ('QUEEN',     9, 'Candidate Queen 9',     'Mining Engineering',      '/images/queen9.jpg'),
    ('PRINCE',    1, 'Candidate Prince 1',    'Computer Science',        '/images/prince1.jpg'),
    ('PRINCE',    2, 'Candidate Prince 2',    'Electronics',             '/images/prince2.jpg'),
    ('PRINCE',    3, 'Candidate Prince 3',    'Mechanical',              '/images/prince3.jpg'),
    ('PRINCE',    4, 'Candidate Prince 4',    'Civil Engineering',       '/images/prince4.jpg'),
    ('PRINCE',    5, 'Candidate Prince 5',    'Architecture',            '/images/prince5.jpg'),
    ('PRINCE',    6, 'Candidate Prince 6',    'Chemical Engineering',    '/images/prince6.jpg'),
    ('PRINCE',    7, 'Candidate Prince 7',    'Information Technology',  '/images/prince7.jpg'),
    ('PRINCE',    8, 'Candidate Prince 8',    'Electrical Engineering',  '/images/prince8.jpg'),
    ('PRINCE',    9, 'Candidate Prince 9',    'Mining Engineering',      '/images/prince9.jpg'),
    ('PRINCESS',  1, 'Candidate Princess 1',  'Computer Science',        '/images/princess1.jpg'),
    ('PRINCESS',  2, 'Candidate Princess 2',  'Electronics',             '/images/princess2.jpg'),
    ('PRINCESS',  3, 'Candidate Princess 3',  'Mechanical',              '/images/princess3.jpg'),
    ('PRINCESS',  4, 'Candidate Princess 4',  'Civil Engineering',       '/images/princess4.jpg'),
    ('PRINCESS',  5, 'Candidate Princess 5',  'Architecture',            '/images/princess5.jpg'),
    ('PRINCESS',  6, 'Candidate Princess 6',  'Chemical Engineering',    '/images/princess6.jpg'),
    ('PRINCESS',  7, 'Candidate Princess 7',  'Information Technology',  '/images/princess7.jpg'),
    ('PRINCESS',  8, 'Candidate Princess 8',  'Electrical Engineering',  '/images/princess8.jpg'),
    ('PRINCESS',  9, 'Candidate Princess 9',  'Mining Engineering',      '/images/princess9.jpg'),
    ('COUPLE',    1, 'Couple 1',              'Computer Science',        '/images/couple1.jpg'),
    ('COUPLE',    2, 'Couple 2',              'Electronics',             '/images/couple2.jpg'),
    ('COUPLE',    3, 'Couple 3',              'Mechanical',              '/images/couple3.jpg'),
    ('COUPLE',    4, 'Couple 4',              'Civil Engineering',       '/images/couple4.jpg'),
    ('COUPLE',    5, 'Couple 5',              'Architecture',            '/images/couple5.jpg'),
    ('COUPLE',    6, 'Couple 6',              'Chemical Engineering',    '/images/couple6.jpg'),
    ('COUPLE',    7, 'Couple 7',              'Information Technology',  '/images/couple7.jpg'),
    ('COUPLE',    8, 'Couple 8',              'Electrical Engineering',  '/images/couple8.jpg'),
    ('COUPLE',    9, 'Couple 9',              'Mining Engineering',      '/images/couple9.jpg')
ON CONFLICT (category, candidate_number)
DO UPDATE SET name = EXCLUDED.name,
              department = EXCLUDED.department,
              image_url = EXCLUDED.image_url;

-- 7) Seed one reusable PIN for smoke tests
INSERT INTO voters (pin, device_id, has_voted)
SELECT '20267', 'shared-pin-seed', FALSE
WHERE NOT EXISTS (SELECT 1 FROM voters WHERE pin = '20267');

COMMIT;
