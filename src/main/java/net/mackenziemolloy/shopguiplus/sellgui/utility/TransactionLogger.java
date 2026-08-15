package net.mackenziemolloy.shopguiplus.sellgui.utility;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.mackenziemolloy.shopguiplus.sellgui.config.PluginConfig;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class TransactionLogger {

    private final JavaPlugin plugin;

    private ExecutorService executor;
    private Logger fileLogger;
    private FileHandler fileHandler;

    public TransactionLogger(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized boolean isEnabled() {
        return this.fileLogger != null;
    }

    public synchronized void apply(@NotNull PluginConfig config) {
        close();

        if (!config.transactionLogEnabled()) {
            return;
        }

        try {
            File logFile = FileUtils.loadFile(this.plugin, "transaction.log");

            FileHandler handler = new FileHandler(logFile.getAbsolutePath(), true);
            handler.setFormatter(new LogFormatter(config.transactionLogDateFormat()));

            Logger logger = Logger.getLogger("SellGUIFileLogger");
            logger.setUseParentHandlers(false);
            for (Handler existing : logger.getHandlers()) {
                logger.removeHandler(existing);
                existing.close();
            }

            logger.addHandler(handler);

            this.fileHandler = handler;
            this.fileLogger = logger;
            this.executor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "SellGUI-TransactionLog");
                thread.setDaemon(true);
                return thread;
            });
        } catch (IOException exception) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to open the transaction log file.", exception);
            this.fileHandler = null;
            this.fileLogger = null;
        }
    }

    public void log(@NotNull String message) {
        ExecutorService currentExecutor;
        Logger currentLogger;

        synchronized (this) {
            currentExecutor = this.executor;
            currentLogger = this.fileLogger;
        }

        if (currentExecutor == null || currentLogger == null) {
            return;
        }

        try {
            currentExecutor.execute(() -> currentLogger.info(message));
        } catch (RejectedExecutionException ignored) {
        }
    }

    public synchronized void close() {
        if (this.executor != null) {
            this.executor.shutdown();

            try {
                if (!this.executor.awaitTermination(2L, TimeUnit.SECONDS)) {
                    this.executor.shutdownNow();
                }
            } catch (InterruptedException ignored) {
                this.executor.shutdownNow();
                Thread.currentThread().interrupt();
            }

            this.executor = null;
        }

        if (this.fileLogger != null) {
            for (Handler handler : this.fileLogger.getHandlers()) {
                this.fileLogger.removeHandler(handler);
                handler.close();
            }

            this.fileLogger = null;
        }

        if (this.fileHandler != null) {
            this.fileHandler.close();
            this.fileHandler = null;
        }
    }
}
