package com.anuraggupta.sqlgenie.repository;

import com.anuraggupta.sqlgenie.config.AbstractIntegrationTest;
import com.anuraggupta.sqlgenie.entity.FavoriteQuery;
import com.anuraggupta.sqlgenie.entity.QueryHistory;
import com.anuraggupta.sqlgenie.entity.QueryStatus;
import com.anuraggupta.sqlgenie.entity.Role;
import com.anuraggupta.sqlgenie.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class QueryHistoryAndFavoritesRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QueryHistoryRepository queryHistoryRepository;
    @Autowired
    private FavoriteQueryRepository favoriteQueryRepository;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(User.builder()
                .email("user-a-" + UUID.randomUUID() + "@example.com")
                .passwordHash("hashed")
                .fullName("User A")
                .role(Role.USER)
                .enabled(true)
                .build());

        userB = userRepository.save(User.builder()
                .email("user-b-" + UUID.randomUUID() + "@example.com")
                .passwordHash("hashed")
                .fullName("User B")
                .role(Role.USER)
                .enabled(true)
                .build());
    }

    private QueryHistory historyFor(User user, String nlQuery) {
        return queryHistoryRepository.save(QueryHistory.builder()
                .user(user)
                .naturalLanguageQuery(nlQuery)
                .generatedSql("SELECT 1")
                .status(QueryStatus.SUCCESS)
                .build());
    }

    @Test
    void findByUserId_returnsOnlyThatUsersHistory_newestFirst() throws InterruptedException {
        historyFor(userA, "first question");
        Thread.sleep(5);
        historyFor(userA, "second question");
        historyFor(userB, "someone else's question");

        Page<QueryHistory> page = queryHistoryRepository.findByUserIdOrderByCreatedAtDesc(
                userA.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getNaturalLanguageQuery()).isEqualTo("second question");
        assertThat(page.getContent().get(1).getNaturalLanguageQuery()).isEqualTo("first question");
    }

    @Test
    void findByIdAndUserId_doesNotLeakOtherUsersRow() {
        QueryHistory row = historyFor(userA, "private question");

        Optional<QueryHistory> asOwner = queryHistoryRepository.findByIdAndUserId(row.getId(), userA.getId());
        Optional<QueryHistory> asOther = queryHistoryRepository.findByIdAndUserId(row.getId(), userB.getId());

        assertThat(asOwner).isPresent();
        assertThat(asOther).isEmpty();
    }

    @Test
    void favoriteQuery_rejectsDuplicateNameForSameUser() {
        favoriteQueryRepository.saveAndFlush(FavoriteQuery.builder()
                .user(userA)
                .name("My favorite")
                .naturalLanguageQuery("top customers")
                .generatedSql("SELECT * FROM customers")
                .build());

        assertThatThrownBy(() -> favoriteQueryRepository.saveAndFlush(FavoriteQuery.builder()
                .user(userA)
                .name("My favorite")
                .naturalLanguageQuery("different question")
                .generatedSql("SELECT * FROM orders")
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void favoriteQuery_allowsSameNameForDifferentUsers() {
        favoriteQueryRepository.saveAndFlush(FavoriteQuery.builder()
                .user(userA)
                .name("Shared name")
                .naturalLanguageQuery("q1")
                .generatedSql("SELECT 1")
                .build());

        favoriteQueryRepository.saveAndFlush(FavoriteQuery.builder()
                .user(userB)
                .name("Shared name")
                .naturalLanguageQuery("q2")
                .generatedSql("SELECT 2")
                .build());

        assertThat(favoriteQueryRepository.existsByUserIdAndName(userA.getId(), "Shared name")).isTrue();
        assertThat(favoriteQueryRepository.existsByUserIdAndName(userB.getId(), "Shared name")).isTrue();
    }

    @Test
    void deletingUser_cascadesToHistoryAndFavorites() {
        historyFor(userA, "will be cascaded");
        favoriteQueryRepository.saveAndFlush(FavoriteQuery.builder()
                .user(userA)
                .name("cascade-fav")
                .naturalLanguageQuery("q")
                .generatedSql("SELECT 1")
                .build());

        UUID userAId = userA.getId();
        userRepository.delete(userA);
        userRepository.flush();

        List<FavoriteQuery> remainingFavorites = favoriteQueryRepository.findByUserIdOrderByCreatedAtDesc(userAId);
        Page<QueryHistory> remainingHistory = queryHistoryRepository.findByUserIdOrderByCreatedAtDesc(
                userAId, PageRequest.of(0, 10));

        assertThat(remainingFavorites).isEmpty();
        assertThat(remainingHistory.getContent()).isEmpty();
    }
}
