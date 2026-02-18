package com.KTU.KTUVotingapp.service;

import com.KTU.KTUVotingapp.dto.VoteRequest;
import com.KTU.KTUVotingapp.model.Candidate;
import com.KTU.KTUVotingapp.model.Category;
import com.KTU.KTUVotingapp.repository.CandidateRepository;
import com.KTU.KTUVotingapp.repository.VoteRepository;
import com.KTU.KTUVotingapp.repository.VoterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "voting.admin-pin=99999",
        "voting.user-pin=12345"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class VotingServiceConcurrencyTest {

    @Autowired
    private VotingService votingService;

    @Autowired
    private VoterRepository voterRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @BeforeEach
    void setUp() {
        // Ensure DB clean
        voteRepository.deleteAll();
        voterRepository.deleteAll();
        candidateRepository.deleteAll();

        // Create candidate for CATEGORY KING with candidateNumber 1
        Candidate candidate = new Candidate(Category.KING, 1, "Test Candidate", "Dept", null);
        candidateRepository.save(candidate);
    }

    @Test
    void concurrentSingleDeviceVoting_shouldOnlyCreateOneVote() throws InterruptedException {
        int threads = 50;
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger conflicts = new AtomicInteger(0);

        List<Runnable> tasks = new ArrayList<>();
        String cookieId = "cookie-123";
        String pin = "12345";

        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                ready.countDown();
                try {
                    start.await();
                    VoteRequest req = new VoteRequest();
                    req.setCookieId(cookieId);
                    req.setPin(pin);
                    req.setCategory(Category.KING);
                    req.setCandidateNumber(1);
                    try {
                        votingService.submitVote(req);
                    } catch (Exception e) {
                        conflicts.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Submit tasks
        for (Runnable r : tasks) exec.submit(r);

        // Wait all threads ready
        ready.await();
        // Start
        start.countDown();

        // Shutdown executor and wait
        exec.shutdown();
        while (!exec.isTerminated()) {
            Thread.sleep(50);
        }

        // Verify only one vote exists for candidate id 1
        Candidate candidate = candidateRepository.findByCategoryAndCandidateNumber(Category.KING, 1).orElseThrow();
        long votesForCandidate = voteRepository.countByCandidateId(candidate.getId());
        assertThat(votesForCandidate).isEqualTo(1);

        // Conflicts should be threads - 1
        assertThat(conflicts.get()).isGreaterThanOrEqualTo(threads - 1);
    }
}
