package server.demo.exception;

import server.demo.dto.RoomTypeDeleteBlockInfo;

public class RoomTypeDeleteBlockedException extends RuntimeException {
    private final RoomTypeDeleteBlockInfo blockInfo;

    public RoomTypeDeleteBlockedException(String message, RoomTypeDeleteBlockInfo blockInfo) {
        super(message);
        this.blockInfo = blockInfo;
    }

    public RoomTypeDeleteBlockInfo getBlockInfo() {
        return blockInfo;
    }
}

