package eu.cymo.kafka.admin;


import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.List;

public class StateConfigDirectoryTest {

    @Test
    @SuppressWarnings("ConstantConditions")
    public void shouldReturnAllYamlFiles() {
        /// given: a state config directory containing 3 yaml files
        String baseDir = getClass()
                .getClassLoader()
                .getResource("test-state-config-dir")
                .getPath();
        var stateConfigDirectory = new StateConfigDirectory(baseDir);

        /// when: the state config files are requested
        List<String> stateConfigs = stateConfigDirectory.getStateConfigs();

        /// then: 3 state config files should've been found
        Assertions.assertThat(stateConfigs).hasSize(3);
    }

    @Test
    public void shouldReturnEmptyListWhenBaseDirDoeNotExists(){
        /// given: a state config directory that does not exists
        var stateConfigDirectory = new StateConfigDirectory("IdoNotExists");

        /// when: the state config files are requested
        List<String> stateConfigs = stateConfigDirectory.getStateConfigs();

        /// then: the result should be empty
        Assertions.assertThat(stateConfigs).isEmpty();
    }

    @Test
    public void shouldNotAllowNullAsBaseDir() {
        var exception = Assertions.catchThrowableOfType(() -> new StateConfigDirectory(null),
                NullPointerException.class);
        Assertions.assertThat(exception.getMessage()).isEqualTo("A baseDir is required and may not be null");
    }
}