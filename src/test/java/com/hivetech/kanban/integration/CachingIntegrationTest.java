package com.hivetech.kanban.integration;

import com.hivetech.kanban.config.CacheConfig;
import com.hivetech.kanban.dto.*;
import com.hivetech.kanban.entity.Task;
import com.hivetech.kanban.enums.Priority;
import com.hivetech.kanban.enums.Status;
import com.hivetech.kanban.repository.TaskRepository;
import com.hivetech.kanban.service.TaskService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for caching behavior in TaskService.
 * Uses PostgreSQL database via Testcontainers and Spring Boot's caching infrastructure to verify:
 * - Cache stores task data correctly
 * - Cache eviction works correctly on mutations
 * - Cache keys are generated properly
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("integration")
@DisplayName("Caching Integration Tests")
class CachingIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kanban_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });

        // Clear database
        taskRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        taskRepository.deleteAll();
    }

    @Nested
    @DisplayName("getTaskById Caching")
    class GetTaskByIdCachingTests {

        @Test
        @DisplayName("should cache task on first call")
        void shouldCacheTaskOnFirstCall() {
            // given
            Task task = createAndSaveTask("Cached Task", Status.TO_DO);

            // when
            taskService.getTaskById(task.getId());

            // then - verify task is in cache
            var cache = cacheManager.getCache(CacheConfig.TASK_CACHE);
            assertThat(cache).isNotNull();
            assertThat(cache.get(task.getId())).isNotNull();
        }

        @Test
        @DisplayName("should return same instance from cache on second call")
        void shouldReturnSameInstanceFromCache() {
            // given
            Task task = createAndSaveTask("Cached Task", Status.TO_DO);

            // when
            TaskResponse firstResult = taskService.getTaskById(task.getId());
            TaskResponse secondResult = taskService.getTaskById(task.getId());

            // then - both calls should return same cached result
            assertThat(firstResult).isNotNull();
            assertThat(secondResult).isNotNull();
            assertThat(firstResult.getId()).isEqualTo(secondResult.getId());
            assertThat(firstResult.getTitle()).isEqualTo(secondResult.getTitle());
        }

        @Test
        @DisplayName("different task IDs should have separate cache entries")
        void differentTaskIdsShouldHaveSeparateCacheEntries() {
            // given
            Task task1 = createAndSaveTask("Task 1", Status.TO_DO);
            Task task2 = createAndSaveTask("Task 2", Status.IN_PROGRESS);

            // when
            TaskResponse result1 = taskService.getTaskById(task1.getId());
            TaskResponse result2 = taskService.getTaskById(task2.getId());

            // then
            var cache = cacheManager.getCache(CacheConfig.TASK_CACHE);
            assertThat(cache).isNotNull();
            assertThat(cache.get(task1.getId())).isNotNull();
            assertThat(cache.get(task2.getId())).isNotNull();
            assertThat(result1.getTitle()).isEqualTo("Task 1");
            assertThat(result2.getTitle()).isEqualTo("Task 2");
        }
    }

    @Nested
    @DisplayName("getAllTasks Caching")
    class GetAllTasksCachingTests {

        @Test
        @DisplayName("should cache paginated tasks on first call")
        void shouldCachePaginatedTasksOnFirstCall() {
            // given
            createAndSaveTask("Task 1", Status.TO_DO);
            createAndSaveTask("Task 2", Status.TO_DO);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<TaskResponse> result = taskService.getAllTasks(null, pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            
            var cache = cacheManager.getCache(CacheConfig.TASKS_CACHE);
            assertThat(cache).isNotNull();
            // Cache key is "null_0_10"
            assertThat(cache.get("null_0_10")).isNotNull();
        }

        @Test
        @DisplayName("should cache different pages separately")
        void shouldCacheDifferentPagesSeparately() {
            // given - create enough tasks for multiple pages
            for (int i = 0; i < 15; i++) {
                createAndSaveTask("Task " + i, Status.TO_DO);
            }
            Pageable page0 = PageRequest.of(0, 10);
            Pageable page1 = PageRequest.of(1, 10);

            // when
            Page<TaskResponse> result0 = taskService.getAllTasks(null, page0);
            Page<TaskResponse> result1 = taskService.getAllTasks(null, page1);

            // then
            assertThat(result0.getContent()).hasSize(10);
            assertThat(result1.getContent()).hasSize(5);
            
            var cache = cacheManager.getCache(CacheConfig.TASKS_CACHE);
            assertThat(cache).isNotNull();
            assertThat(cache.get("null_0_10")).isNotNull();
            assertThat(cache.get("null_1_10")).isNotNull();
        }

        @Test
        @DisplayName("should cache by status filter")
        void shouldCacheByStatusFilter() {
            // given
            createAndSaveTask("Todo Task", Status.TO_DO);
            createAndSaveTask("Done Task", Status.DONE);
            Pageable pageable = PageRequest.of(0, 10);

            // when
            Page<TaskResponse> todoTasks = taskService.getAllTasks(Status.TO_DO, pageable);
            Page<TaskResponse> doneTasks = taskService.getAllTasks(Status.DONE, pageable);

            // then
            assertThat(todoTasks.getContent()).hasSize(1);
            assertThat(doneTasks.getContent()).hasSize(1);
            
            var cache = cacheManager.getCache(CacheConfig.TASKS_CACHE);
            assertThat(cache).isNotNull();
            // Different cache keys for different statuses
            assertThat(cache.get("TO_DO_0_10")).isNotNull();
            assertThat(cache.get("DONE_0_10")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Cache Eviction on Create")
    class CacheEvictionOnCreateTests {

        @Test
        @DisplayName("should evict tasks cache when creating new task")
        void shouldEvictTasksCacheWhenCreatingNewTask() {
            // given - populate cache
            createAndSaveTask("Existing Task", Status.TO_DO);
            Pageable pageable = PageRequest.of(0, 10);
            taskService.getAllTasks(null, pageable);
            
            var cache = cacheManager.getCache(CacheConfig.TASKS_CACHE);
            assertThat(cache).isNotNull();
            assertThat(cache.get("null_0_10")).isNotNull();

            // when - create new task
            TaskRequest request = TaskRequest.builder()
                    .title("New Task")
                    .description("New Description")
                    .status("TO_DO")
                    .priority("MEDIUM")
                    .build();
            taskService.createTask(request);

            // then - cache should be evicted
            assertThat(cache.get("null_0_10")).isNull();
        }
    }

    @Nested
    @DisplayName("Cache Eviction on Update")
    class CacheEvictionOnUpdateTests {

        @Test
        @DisplayName("should evict both caches when updating task")
        void shouldEvictBothCachesWhenUpdatingTask() {
            // given
            Task task = createAndSaveTask("Original Title", Status.TO_DO);
            Pageable pageable = PageRequest.of(0, 10);
            
            // Populate both caches
            taskService.getTaskById(task.getId());
            taskService.getAllTasks(null, pageable);
            
            var taskCache = cacheManager.getCache(CacheConfig.TASK_CACHE);
            var tasksCache = cacheManager.getCache(CacheConfig.TASKS_CACHE);
            assertThat(taskCache.get(task.getId())).isNotNull();
            assertThat(tasksCache.get("null_0_10")).isNotNull();

            // when - update task
            TaskUpdateRequest updateRequest = TaskUpdateRequest.builder()
                    .title("Updated Title")
                    .description("Updated Description")
                    .status("IN_PROGRESS")
                    .priority("HIGH")
                    .version(task.getVersion())
                    .build();
            taskService.updateTask(task.getId(), updateRequest);

            // then - both caches should be evicted
            assertThat(taskCache.get(task.getId())).isNull();
            assertThat(tasksCache.get("null_0_10")).isNull();
        }
    }

    @Nested
    @DisplayName("Cache Eviction on Delete")
    class CacheEvictionOnDeleteTests {

        @Test
        @DisplayName("should evict both caches when deleting task")
        void shouldEvictBothCachesWhenDeletingTask() {
            // given
            Task task = createAndSaveTask("Task to Delete", Status.TO_DO);
            Pageable pageable = PageRequest.of(0, 10);
            
            // Populate both caches
            taskService.getTaskById(task.getId());
            taskService.getAllTasks(null, pageable);
            
            var taskCache = cacheManager.getCache(CacheConfig.TASK_CACHE);
            var tasksCache = cacheManager.getCache(CacheConfig.TASKS_CACHE);
            assertThat(taskCache.get(task.getId())).isNotNull();
            assertThat(tasksCache.get("null_0_10")).isNotNull();

            // when - delete task
            taskService.deleteTask(task.getId());

            // then - both caches should be evicted
            assertThat(taskCache.get(task.getId())).isNull();
            assertThat(tasksCache.get("null_0_10")).isNull();
        }
    }

    @Nested
    @DisplayName("Cache Eviction on Patch")
    class CacheEvictionOnPatchTests {

        @Test
        @DisplayName("should evict both caches when patching task")
        void shouldEvictBothCachesWhenPatchingTask() {
            // given
            Task task = createAndSaveTask("Task to Patch", Status.TO_DO);
            Pageable pageable = PageRequest.of(0, 10);
            
            // Populate caches
            taskService.getTaskById(task.getId());
            taskService.getAllTasks(null, pageable);
            
            var taskCache = cacheManager.getCache(CacheConfig.TASK_CACHE);
            var tasksCache = cacheManager.getCache(CacheConfig.TASKS_CACHE);
            assertThat(taskCache.get(task.getId())).isNotNull();
            assertThat(tasksCache.get("null_0_10")).isNotNull();

            // when - patch task
            TaskPatchRequest patchRequest = TaskPatchRequest.builder()
                    .status("DONE")
                    .build();
            taskService.patchTask(task.getId(), patchRequest);

            // then - both caches should be evicted
            assertThat(taskCache.get(task.getId())).isNull();
            assertThat(tasksCache.get("null_0_10")).isNull();
        }
    }

    @Nested
    @DisplayName("Cache Key Generation")
    class CacheKeyGenerationTests {

        @Test
        @DisplayName("getAllTasks with same parameters should use same cache key")
        void getAllTasksWithSameParametersShouldUseSameCacheKey() {
            // given
            createAndSaveTask("Test Task", Status.TO_DO);
            Pageable pageable = PageRequest.of(0, 10);

            // when - call twice with same parameters
            Page<TaskResponse> result1 = taskService.getAllTasks(Status.TO_DO, pageable);
            Page<TaskResponse> result2 = taskService.getAllTasks(Status.TO_DO, pageable);

            // then - same cache key should be used
            var cache = cacheManager.getCache(CacheConfig.TASKS_CACHE);
            assertThat(cache).isNotNull();
            assertThat(cache.get("TO_DO_0_10")).isNotNull();
            assertThat(result1.getTotalElements()).isEqualTo(result2.getTotalElements());
        }

        @Test
        @DisplayName("getAllTasks with different page sizes should use different cache keys")
        void getAllTasksWithDifferentPageSizesShouldUseDifferentCacheKeys() {
            // given
            createAndSaveTask("Test Task", Status.TO_DO);
            Pageable pageable10 = PageRequest.of(0, 10);
            Pageable pageable20 = PageRequest.of(0, 20);

            // when
            taskService.getAllTasks(null, pageable10);
            taskService.getAllTasks(null, pageable20);

            // then - different cache keys
            var cache = cacheManager.getCache(CacheConfig.TASKS_CACHE);
            assertThat(cache).isNotNull();
            assertThat(cache.get("null_0_10")).isNotNull();
            assertThat(cache.get("null_0_20")).isNotNull();
        }
    }

    private Task createAndSaveTask(String title, Status status) {
        Task task = Task.builder()
                .title(title)
                .description("Test Description")
                .status(status)
                .priority(Priority.MEDIUM)
                .build();
        return taskRepository.saveAndFlush(task);
    }
}

