package de.acepe.fritzstreams;

import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FutureTest {
    private static final Logger LOG = LoggerFactory.getLogger(FutureTest.class);

    private JLabel result;
    private Executor backend;
    private Executor ui;
    private JFrame frame;
    private JLabel countLabel;
    private double count;
    private JLabel result2;
    private JTextField input;

    public static void main(String... args) {
        SwingUtilities.invokeLater(() -> new FutureTest().test());
    }

    private void test() {
        setupUI();
        setupExecutors();
        frame.setVisible(true);
    }

    private void doWork(ActionEvent actionEvent) {
        String inputText = input.getText();

        CompletableFuture<String> computeValue = CompletableFuture.supplyAsync(() -> computeValue(inputText), backend)
                                                                  .handle(this::handle)
                                                                  .exceptionally(this::failed);

        CompletableFuture<Integer> intFuture = CompletableFuture.supplyAsync(this::computeInt, backend);

        CompletableFuture<Foo> fooFuture = computeValue.thenCombine(intFuture, this::createFoo);
        fooFuture.thenAcceptAsync(this::onSuccess, ui);

        CompletableFuture<String> workerCallback = new CompletableFuture<>();
        new MeaningOfLifeFinder(workerCallback).execute();

        workerCallback.thenAcceptAsync(this::onSuccess, ui).thenRunAsync(this::makeRed, ui);
    }

    private String computeValue(String param) {
        LOG.info("computing String...");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return String.valueOf(Integer.parseInt(param) * 2);
    }

    private Integer computeInt() {
        LOG.info("computing int...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return (int) (Math.random() * 1000);
    }

    private Foo createFoo(String s, Integer i) {
        LOG.info("computing foo...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new Foo(s, i);
    }

    private void onSuccess(String result) {
        LOG.info("String success: " + result);
        this.result.setText(result);
    }

    private void onSuccess(Foo result) {
        LOG.info("Foo success: " + result);
        this.result2.setText(result.toString());
    }

    private void makeRed() {
        LOG.info("making red");
        result.setForeground(Color.red);
        result2.setForeground(Color.red);
    }

    private String handle(String value, Throwable throwable) {
        if (!value.equals("84")) {
            LOG.error("Exception in Thread: " + Thread.currentThread(), throwable);
            throw new RuntimeException("Geht nicht weiter");
        }
        return value;
    }

    private String failed(Throwable throwable) {
        LOG.error("Exception in Thread: " + Thread.currentThread(), throwable);
        return "oh, we failed";
    }

    private void setupExecutors() {
        backend = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("Worker");
            return t;
        });

        ui = runnable -> {
            if (SwingUtilities.isEventDispatchThread()) {
                LOG.info("already in EDT");
                runnable.run();
            } else {
                LOG.info("scheduling in EDT");
                SwingUtilities.invokeLater(runnable);
            }
        };
    }

    private void setupUI() {
        result = new JLabel("-");
        result2 = new JLabel("-");
        countLabel = new JLabel("0");
        input = new JTextField("42");
        JButton execButton = new JButton("Compute");
        execButton.addActionListener(this::doWork);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(countLabel, BorderLayout.NORTH);
        panel.add(result, BorderLayout.WEST);
        panel.add(result2, BorderLayout.EAST);
        panel.add(input, BorderLayout.CENTER);
        panel.add(execButton, BorderLayout.SOUTH);

        frame = new JFrame("Future-Test");
        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setSize(new Dimension(200, 200));

        Timer countTimer = new Timer(100, e -> {
            count += 0.1;
            countLabel.setText(new DecimalFormat("#0.00").format(count));
        });
        countTimer.setRepeats(true);
        countTimer.start();
    }


    class MeaningOfLifeFinder extends SwingWorker<String, Object> {
        private final CompletableFuture<String> callbackFuture;

        public MeaningOfLifeFinder(CompletableFuture<String> callbackFuture) {
            this.callbackFuture = callbackFuture;
        }

        @Override
        public String doInBackground(){
            LOG.info("computing answer to everything...");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "42";
        }

        @Override
        protected void done() {
            LOG.info("Swing-Worker is done");
            try {
                callbackFuture.complete(get());
            } catch (Exception ignore) {
            }
        }
    }

    private static class Foo {
        String s;
        Integer i;

        public Foo(String s, Integer i) {
            this.s = s;
            this.i = i;
        }

        @Override
        public String toString() {
            return s + ":" + i;
        }
    }
}
