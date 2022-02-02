package org.roda.rodain.core.shallowSipManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.roda.rodain.core.ConfigurationManager;
import org.roda.rodain.core.Constants;

/**
 * {@author João Gomes <jgomes@keep.pt>}.
 */
public abstract class UriCreator implements UriCreatorInterface {

  /**
   * Get SIP shallow SIP Configurations.
   * 
   * @return {@link List}
   */
  public static List<Configuration> getShallowSipConfigurations() {
    final String configListRaw = ConfigurationManager.getConfig(Constants.CONF_K_REFERENCE_TRANSFORMER_LIST);
    final List<Configuration> configurationList = new ArrayList<>();
    if (configListRaw != null) {
      final String[] configList = configListRaw.split(",");
      for (String config : configList) {
        final String sourceBasepath = ConfigurationManager
          .getConfig(Constants.CONF_K_REFERENCE_TRANSFORMER + config + ".basepath");
        final String targetBasepath = ConfigurationManager
          .getConfig(Constants.CONF_K_REFERENCE_TRANSFORMER + config + ".targetPath");
        final String host = ConfigurationManager.getConfig(Constants.CONF_K_REFERENCE_TRANSFORMER + config + ".host");
        final String protocol = ConfigurationManager
          .getConfig(Constants.CONF_K_REFERENCE_TRANSFORMER + config + ".protocol");
        final String port = ConfigurationManager.getConfig(Constants.CONF_K_REFERENCE_TRANSFORMER + config + ".port");

        configurationList.add(
          new Configuration(sourceBasepath, targetBasepath != null ? Optional.of(targetBasepath) : Optional.empty(),
            host != null ? Optional.of(host) : Optional.empty(), protocol,
            port != null ? Optional.of(port) : Optional.empty()));
      }
    }
    return configurationList;
  }
}
