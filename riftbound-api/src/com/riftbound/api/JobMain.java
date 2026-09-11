package com.riftbound.api;

import java.nio.file.Path;
import java.util.List;

public final class JobMain {
    public static void main(String[] args) {
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
            System.out.println("emails=" + new EmailNotifier(store, mailer).send(hits));
        } catch (Exception e) {
            System.err.println("search job aborted: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
