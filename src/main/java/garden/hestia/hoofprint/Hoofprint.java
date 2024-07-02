package garden.hestia.hoofprint;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hoofprint implements ClientModInitializer {
    public static final String ID = "hoofprint";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Hoofprint] They went thatta-way!");
    }
}
