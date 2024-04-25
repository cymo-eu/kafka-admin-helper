package eu.cymo.kafka.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StateConfigDirectory {

    private static final Logger log = LoggerFactory.getLogger(StateConfigDirectory.class);

    private final String baseDir;

    public StateConfigDirectory(String baseDir) {
        this.baseDir = Objects.requireNonNull(baseDir, "A baseDir is required and may not be null");
    }

    public List<String> getStateConfigs() {
        if (Files.exists(Path.of(baseDir))) {
            try {
                return Files.walk(Paths.get(baseDir))
                        .filter(Files::isRegularFile)
                        .filter(f -> f.getFileName().toString().endsWith(".yaml") || f.getFileName().toString().endsWith(".yml"))
                        .map(f -> f.toAbsolutePath().toString())
                        .toList();
            } catch (IOException e) {
                throw new RuntimeException("Unable to read state config file.", e);
            }
        } else {
            log.warn("BaseDir {} does not exists so no extra state config files found", baseDir);
            return Collections.emptyList();
        }
    }
}
