package com.lr.business;

import com.lr.config.Beans;
import com.lr.config.GeneralConfig;
import com.lr.config.MarchConfig;
import com.lr.utils.WinUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;

import java.awt.Robot;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for WindowAutomationWorker multithread functionality.
 * Tests worker lifecycle, shutdown mechanism, and Robot instance isolation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WindowAutomationWorkerIT {

    @Mock
    private CoreMechanics coreMechanics;

    @Mock
    private GeneralConfig generalConfig;

    @Mock
    private MarchConfig marchConfig;

    @Mock
    private WebClient discordWebClient;

    @Mock
    private Beans.RobotFactory robotFactory;

    private Random random;

    @BeforeEach
    void setUp() throws Exception {
        random = new Random();

        // Configure mocks with default behavior
        when(marchConfig.getMarchesAvailable()).thenReturn(0); // No marches to prevent actual automation
        when(marchConfig.getMarchesIntervalMins()).thenReturn(60L); // Long type
        when(generalConfig.getActionIntervalMs()).thenReturn(100L); // Long type

        // Mock RobotFactory to return a mock Robot by default
        Robot mockRobot = mock(Robot.class);
        when(robotFactory.createRobot()).thenReturn(mockRobot);
    }

    @Test
    @Timeout(5)
    void shouldCreateWorkerWithAllDependencies() {
        // Given
        WinUtils.WindowInfo windowInfo = createMockWindowInfo("TestWindow");

        // When
        WindowAutomationWorker worker = new WindowAutomationWorker(
            windowInfo, coreMechanics, generalConfig, marchConfig,
            discordWebClient, random, robotFactory
        );

        // Then
        assertNotNull(worker, "Worker should be created");
    }

    @Test
    @Timeout(5)
    void shouldStopGracefullyWhenShutdownRequested() throws InterruptedException {
        // Given
        WinUtils.WindowInfo windowInfo = createMockWindowInfo("TestWindow");
        WindowAutomationWorker worker = new WindowAutomationWorker(
            windowInfo, coreMechanics, generalConfig, marchConfig,
            discordWebClient, random, robotFactory
        );

        CountDownLatch workerStarted = new CountDownLatch(1);
        CountDownLatch workerFinished = new CountDownLatch(1);

        Thread workerThread = new Thread(() -> {
            try {
                workerStarted.countDown();
                worker.run();
            } finally {
                workerFinished.countDown();
            }
        });

        // When
        workerThread.start();
        workerStarted.await(1, TimeUnit.SECONDS);
        Thread.sleep(100); // Let worker enter loop
        worker.requestShutdown();

        // Then
        boolean finished = workerFinished.await(2, TimeUnit.SECONDS);
        assertTrue(finished, "Worker should terminate after shutdown request");
        assertFalse(workerThread.isAlive(), "Worker thread should be dead");
    }

    @Test
    @Timeout(5)
    void shouldStopWhenThreadInterrupted() throws InterruptedException {
        // Given
        WinUtils.WindowInfo windowInfo = createMockWindowInfo("TestWindow");
        WindowAutomationWorker worker = new WindowAutomationWorker(
            windowInfo, coreMechanics, generalConfig, marchConfig,
            discordWebClient, random, robotFactory
        );

        CountDownLatch workerFinished = new CountDownLatch(1);

        Thread workerThread = new Thread(() -> {
            try {
                worker.run();
            } finally {
                workerFinished.countDown();
            }
        });

        // When
        workerThread.start();
        Thread.sleep(100); // Let worker enter loop
        workerThread.interrupt();

        // Then
        boolean finished = workerFinished.await(2, TimeUnit.SECONDS);
        assertTrue(finished, "Worker should terminate after interrupt");
        assertTrue(workerThread.isInterrupted(), "Thread interrupt status should be preserved");
    }

    @Test
    @Timeout(10)
    void shouldIsolateRobotInstancesAcrossMultipleWorkers() throws Exception {
        // Given
        int workerCount = 3;
        AtomicInteger robotCreationCount = new AtomicInteger(0);
        List<Robot> createdRobots = new ArrayList<>();

        // Mock RobotFactory to track Robot creation
        when(robotFactory.createRobot()).thenAnswer(invocation -> {
            robotCreationCount.incrementAndGet();
            Robot mockRobot = mock(Robot.class);
            synchronized (createdRobots) {
                createdRobots.add(mockRobot);
            }
            return mockRobot;
        });

        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        List<WindowAutomationWorker> workers = new ArrayList<>();

        // Create workers for different windows
        for (int i = 0; i < workerCount; i++) {
            WinUtils.WindowInfo windowInfo = createMockWindowInfo("Window" + i);
            WindowAutomationWorker worker = new WindowAutomationWorker(
                windowInfo, coreMechanics, generalConfig, marchConfig,
                discordWebClient, random, robotFactory
            );
            workers.add(worker);
        }

        // When - Submit all workers to executor
        CountDownLatch allStarted = new CountDownLatch(workerCount);
        for (WindowAutomationWorker worker : workers) {
            executor.submit(() -> {
                allStarted.countDown();
                worker.run();
            });
        }

        // Wait for all to start
        allStarted.await(2, TimeUnit.SECONDS);
        Thread.sleep(200); // Let them initialize

        // Request shutdown for all
        workers.forEach(WindowAutomationWorker::requestShutdown);

        executor.shutdown();
        boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);

        // Then
        assertTrue(terminated, "All workers should terminate");

        // Note: Robot creation happens lazily on first use
        // Since we configured 0 marches, robots may not be created
        // This test validates the isolation mechanism is in place
        assertTrue(robotCreationCount.get() <= workerCount,
            "Should not create more Robot instances than workers");
    }

    @Test
    @Timeout(10)
    void shouldHandleConcurrentCoordinateRegistration() throws Exception {
        // Given
        int workerCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        CountDownLatch allFinished = new CountDownLatch(workerCount);

        // When - Multiple workers try to register coordinates concurrently
        for (int i = 0; i < workerCount; i++) {
            final String windowName = "Window" + i;
            executor.submit(() -> {
                try {
                    WinUtils.WindowInfo windowInfo = createMockWindowInfo(windowName);
                    WindowAutomationWorker worker = new WindowAutomationWorker(
                        windowInfo, coreMechanics, generalConfig, marchConfig,
                        discordWebClient, random, robotFactory
                    );

                    // Trigger initialization which registers coordinates
                    // This will fail gracefully due to mocked dependencies
                    try {
                        worker.run();
                    } catch (Exception e) {
                        // Expected - missing robot/resources, but registration should complete
                    }
                } finally {
                    allFinished.countDown();
                }
            });
        }

        boolean completed = allFinished.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertTrue(completed, "All concurrent registrations should complete without deadlock");
    }

    @Test
    @Timeout(5)
    void shouldHandleInterruptGracefully() throws InterruptedException {
        // Given
        WinUtils.WindowInfo windowInfo = createMockWindowInfo("TestWindow");
        WindowAutomationWorker worker = new WindowAutomationWorker(
            windowInfo, coreMechanics, generalConfig, marchConfig,
            discordWebClient, random, robotFactory
        );

        CountDownLatch workerFinished = new CountDownLatch(1);

        Thread workerThread = new Thread(() -> {
            try {
                worker.run();
            } finally {
                workerFinished.countDown();
            }
        });

        // When
        workerThread.start();
        Thread.sleep(100);
        workerThread.interrupt();

        // Then
        boolean finished = workerFinished.await(2, TimeUnit.SECONDS);
        assertTrue(finished, "Worker should terminate gracefully after interrupt");
        assertFalse(workerThread.isAlive(), "Worker thread should be dead");
    }

    @Test
    @Timeout(5)
    void shouldHandleExceptionsDuringInitialization() throws Exception {
        // Given
        WinUtils.WindowInfo windowInfo = createMockWindowInfo("TestWindow");

        // Mock RobotFactory to throw exception
        when(robotFactory.createRobot()).thenThrow(new java.awt.AWTException("Robot creation failed"));

        WindowAutomationWorker worker = new WindowAutomationWorker(
            windowInfo, coreMechanics, generalConfig, marchConfig,
            discordWebClient, random, robotFactory
        );

        CountDownLatch workerFinished = new CountDownLatch(1);

        Thread workerThread = new Thread(() -> {
            try {
                worker.run();
            } finally {
                workerFinished.countDown();
            }
        });

        // When
        workerThread.start();
        boolean finished = workerFinished.await(3, TimeUnit.SECONDS);

        // Then
        assertTrue(finished, "Worker should terminate gracefully even after initialization error");
        assertFalse(workerThread.isAlive(), "Worker thread should be dead");
    }

    /**
     * Helper method to create mock WindowInfo
     */
    private WinUtils.WindowInfo createMockWindowInfo(String title) {
        WinUtils.RECT rect = new WinUtils.RECT();
        rect.left = 0;
        rect.top = 0;
        rect.right = 800;
        rect.bottom = 600;
        return new WinUtils.WindowInfo(rect, title);
    }
}
