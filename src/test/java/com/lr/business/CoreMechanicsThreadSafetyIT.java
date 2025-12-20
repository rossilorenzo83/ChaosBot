package com.lr.business;

import com.lr.config.GeneralConfig;
import com.lr.utils.WindowInputService;
import com.lr.utils.WinUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ResourceLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Thread safety integration tests for CoreMechanics.
 * Tests concurrent access to shared data structures and synchronization mechanisms.
 */
@ExtendWith(MockitoExtension.class)
class CoreMechanicsThreadSafetyIT {

    @Mock
    private net.sourceforge.tess4j.Tesseract ocrEngine;

    @Mock
    private GeneralConfig generalConfig;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private WindowInputService windowInputService;

    private CoreMechanics coreMechanics;

    @BeforeEach
    void setUp() {
        coreMechanics = new CoreMechanics(ocrEngine, generalConfig, resourceLoader, windowInputService);
    }

    @Test
    @Timeout(10)
    void shouldSafelyInitializeMainMapButtonsCoordsMapConcurrently() throws Exception {
        // Given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        // When - Multiple threads try to access/initialize the map concurrently
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // All threads start at the same time
                    ConcurrentMap<String, Map<MainMapButtons, Double[]>> map =
                        coreMechanics.getMainMapButtonsCoordsMap();

                    // First access might be null, triggering initialization elsewhere
                    // This tests the race condition handling
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all threads
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertTrue(completed, "All threads should complete without deadlock");
        assertTrue(exceptions.isEmpty(), "No exceptions should occur during concurrent access");
    }

    @Test
    @Timeout(10)
    void shouldSafelySetAndGetMainMapButtonsCoordsMapConcurrently() throws Exception {
        // Given
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ConcurrentMap<String, Map<MainMapButtons, Double[]>> initialMap = new ConcurrentHashMap<>();
        coreMechanics.setMainMapButtonsCoordsMap(initialMap);

        // When - Multiple threads set and get the map concurrently
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // Each thread creates its own map with window-specific data
                    ConcurrentMap<String, Map<MainMapButtons, Double[]>> threadMap =
                        new ConcurrentHashMap<>();
                    threadMap.put("Window" + threadId, new ConcurrentHashMap<>());

                    // Get current map
                    ConcurrentMap<String, Map<MainMapButtons, Double[]>> currentMap =
                        coreMechanics.getMainMapButtonsCoordsMap();

                    assertNotNull(currentMap, "Map should never be null after initialization");

                } catch (Exception e) {
                    fail("Thread " + threadId + " failed: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertTrue(completed, "All concurrent operations should complete");
        assertNotNull(coreMechanics.getMainMapButtonsCoordsMap(),
            "Map should be initialized after concurrent access");
    }

    @Test
    @Timeout(10)
    void shouldHandleConcurrentWindowCoordinateRegistration() throws Exception {
        // Given
        int windowCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(windowCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(windowCount);

        // Initialize the map first
        ConcurrentMap<String, Map<MainMapButtons, Double[]>> coordsMap = new ConcurrentHashMap<>();
        coreMechanics.setMainMapButtonsCoordsMap(coordsMap);

        // When - Multiple threads register coordinates for different windows
        for (int i = 0; i < windowCount; i++) {
            final String windowName = "Window" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // Simulate coordinate registration
                    Map<MainMapButtons, Double[]> windowCoords = new ConcurrentHashMap<>();
                    windowCoords.put(MainMapButtons.SEARCH, new Double[]{100.0, 200.0});

                    ConcurrentMap<String, Map<MainMapButtons, Double[]>> map =
                        coreMechanics.getMainMapButtonsCoordsMap();
                    map.put(windowName, windowCoords);

                } catch (Exception e) {
                    fail("Failed to register coordinates for " + windowName + ": " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertTrue(completed, "All coordinate registrations should complete");
        assertEquals(windowCount, coreMechanics.getMainMapButtonsCoordsMap().size(),
            "All windows should be registered");
    }

    @Test
    @Timeout(10)
    void shouldProvideThreadSafeCoordsMapInitLock() {
        // Given/When
        Object lock1 = coreMechanics.getCoordsMapInitLock();
        Object lock2 = coreMechanics.getCoordsMapInitLock();

        // Then
        assertNotNull(lock1, "Lock should not be null");
        assertSame(lock1, lock2, "Lock should be the same instance across calls");
    }

    @Test
    @Timeout(10)
    void shouldHandleNullMapGracefully() {
        // Given
        coreMechanics.setMainMapButtonsCoordsMap(null);

        // When
        ConcurrentMap<String, Map<MainMapButtons, Double[]>> result =
            coreMechanics.getMainMapButtonsCoordsMap();

        // Then
        assertNull(result, "Should return null if map was set to null");
    }

    @Test
    @Timeout(10)
    void shouldMaintainMapConsistencyUnderConcurrentModification() throws Exception {
        // Given
        int operationCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        ConcurrentMap<String, Map<MainMapButtons, Double[]>> coordsMap = new ConcurrentHashMap<>();
        coreMechanics.setMainMapButtonsCoordsMap(coordsMap);

        CountDownLatch doneLatch = new CountDownLatch(operationCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When - Perform many concurrent put operations
        for (int i = 0; i < operationCount; i++) {
            final int opId = i;
            executor.submit(() -> {
                try {
                    String windowName = "Window" + (opId % 10); // 10 different windows
                    Map<MainMapButtons, Double[]> coords = new ConcurrentHashMap<>();
                    coords.put(MainMapButtons.SEARCH, new Double[]{opId * 1.0, opId * 2.0});

                    ConcurrentMap<String, Map<MainMapButtons, Double[]>> map =
                        coreMechanics.getMainMapButtonsCoordsMap();
                    map.put(windowName, coords);

                    successCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertTrue(completed, "All operations should complete");
        assertEquals(operationCount, successCount.get(), "All operations should succeed");
        assertTrue(coreMechanics.getMainMapButtonsCoordsMap().size() <= 10,
            "Map should contain at most 10 unique windows");
    }

    @Test
    @Timeout(10)
    void shouldSupportConcurrentReadsWhileInitializing() throws Exception {
        // Given
        int readerCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(readerCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(readerCount);
        List<ConcurrentMap<String, Map<MainMapButtons, Double[]>>> results =
            new CopyOnWriteArrayList<>();

        // When - Multiple threads try to read while map might be null
        for (int i = 0; i < readerCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ConcurrentMap<String, Map<MainMapButtons, Double[]>> map =
                        coreMechanics.getMainMapButtonsCoordsMap();
                    results.add(map);
                } catch (Exception e) {
                    fail("Read operation failed: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertTrue(completed, "All read operations should complete");
        assertEquals(readerCount, results.size(), "All readers should complete");

        // All results should be either null (if read before initialization) or the same instance
        ConcurrentMap<String, Map<MainMapButtons, Double[]>> nonNullMap = null;
        for (ConcurrentMap<String, Map<MainMapButtons, Double[]>> result : results) {
            if (result != null) {
                if (nonNullMap == null) {
                    nonNullMap = result;
                } else {
                    assertSame(nonNullMap, result, "All non-null results should reference the same map");
                }
            }
        }
    }
}
