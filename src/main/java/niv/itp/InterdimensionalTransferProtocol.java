package niv.itp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

public class InterdimensionalTransferProtocol implements ModInitializer {

    public static final String MOD_ID = "interdimensional-transfer-protocol";

    public static final String MOD_NAME = "Interdimensional Transfer Protocol";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("({}) Ready", MOD_NAME);
    }
}
