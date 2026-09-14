package com.riftbound.api;

import java.nio.file.Path;
import java.util.List;

public final class JobMain {
    public static void main(String[] args) {
        if (args.length >= 2 && "--mail-test".equals(args[0])) {
            try {
                Mailer mailer = Mailer.fromEnv();
                if (mailer == null) {
                    System.err.println("Set SMTP_HOST and MAIL_FROM in .env first");
                    return;
                }
                mailer.send(args[1], "tt-watcher test", "If you got this, SMTP works.\nhttps://ttwatcher.com\n");
                System.out.println("sent test to " + args[1]);
            } catch (Exception e) {
                System.err.println("mail-test failed: " + e.getMessage());
                e.printStackTrace();
            }
            return;
        }
        try {
            Path storeFile = ApiMain.dataFile();
            Store store = new Store(storeFile);
            SearchJob job = new SearchJob(store, InternalEventsClient.fromEnv());
            System.out.println("search job store=" + storeFile.toAbsolutePath());
            List<SearchJob.Hit> hits = job.run();
            System.out.println("matches=" + hits.size());
            for (SearchJob.Hit hit : hits) System.out.println("  " + hit.line());
            Mailer mailer = Mailer.fromEnv();
            if (mailer == null) System.out.println("SMTP_HOST/MAIL_FROM not set; printing emails only");
            EmailNotifier.Result result = new EmailNotifier(store, mailer).process(hits);
            System.out.println("emails=" + result.sent() + " removed=" + result.removed());
        } catch (Exception e) {
            System.err.println("search job aborted: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
