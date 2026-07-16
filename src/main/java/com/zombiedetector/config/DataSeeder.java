package com.zombiedetector.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.zombiedetector.model.ManagedNode;
import com.zombiedetector.repository.NodeRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final NodeRepository nodeRepository;

    public DataSeeder(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @Override
    public void run(String... args) {
        if (nodeRepository.count() > 0) return; // guard against re-seeding on restart

        String[] environments = {"PROD", "STAGING", "DEV"};
        String[] types = {"t2.micro", "t2.large", "m5.large", "c5.xlarge"};
        double[] rates = {0.0116, 0.0928, 0.096, 0.17};

        for (int i = 1; i <= 20; i++) {
            int typeIdx = i % types.length;
            int envIdx = i % environments.length;
            ManagedNode node = new ManagedNode(
                "node-" + i,
                types[typeIdx],
                rates[typeIdx],
                environments[envIdx]
            );
            nodeRepository.save(node);
        }
        System.out.println("Seeded 20 nodes.");
    }
}