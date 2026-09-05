package com.hbm.tileentity.network;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.radio.ModRadioBlockEntities;
import com.hbm.radio.RadioTelexMenu;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The complete server-side TELEX state machine, ported from 1.12.2's {@code TileEntityRadioTelex}.
 * Characters are broadcast one at a time; {@link RTTYSystem} supplies the old one-tick delivery band.
 */
public final class TileEntityRadioTelex extends TileEntityLoadedBase implements IControlReceiver, MenuProvider {
    public static final int LINE_WIDTH = 33;
    public static final int LINE_COUNT = 5;
    public static final int CHANNEL_LENGTH = 10;

    public String txChannel = "";
    public String rxChannel = "";
    public final String[] txBuffer = new String[]{"", "", "", "", ""};
    public final String[] rxBuffer = new String[]{"", "", "", "", ""};
    public int sendingLine;
    public int sendingIndex;
    public boolean isSending;
    public int sendingWait;
    public int writingLine;
    public boolean printAfterRx;
    public boolean deleteOnReceive = true;
    public char sendingChar = ' ';

    public static final char EOL = '\n';
    public static final char EOT = '\u0004';
    public static final char BELL = '\u0007';
    public static final char PRINT = '\u000c';
    public static final char PAUSE = '\u0016';
    public static final char CLEAR = '\u007f';

    public TileEntityRadioTelex(BlockPos pos, BlockState state) {
        super(ModRadioBlockEntities.RADIO_TELEX.get(), pos, state);
    }

    /** Runs on the server ticker. The order is intentionally send first, then receive, then sync. */
    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        sendingChar = ' ';

        if (isSending && value(txChannel).isEmpty()) isSending = false;

        if (isSending) {
            if (sendingWait > 0) {
                sendingWait--;
            } else {
                String line = value(txBuffer[sendingLine]);
                if (line.length() > sendingIndex) {
                    char character = line.charAt(sendingIndex++);
                    if (character == PAUSE) {
                        sendingWait = 20;
                    } else {
                        RTTYSystem.broadcast(level, txChannel, character);
                        sendingChar = character;
                    }
                } else {
                    if (sendingLine >= LINE_COUNT - 1) {
                        isSending = false;
                        RTTYSystem.broadcast(level, txChannel, EOT);
                        sendingLine = 0;
                    } else {
                        RTTYSystem.broadcast(level, txChannel, EOL);
                        sendingLine++;
                    }
                    sendingIndex = 0;
                }
            }
        }

        if (!rxChannel.isEmpty()) {
            RTTYSystem.RTTYChannel channel = RTTYSystem.listen(level, rxChannel);
            if (channel != null && channel.signal instanceof Character
                    && channel.timeStamp > level.getGameTime() - 2 && channel.timeStamp != -1) {
                char character = (char) channel.signal;

                if (deleteOnReceive) {
                    deleteOnReceive = false;
                    for (int i = 0; i < LINE_COUNT; i++) rxBuffer[i] = "";
                    writingLine = 0;
                }

                if (character == EOT) {
                    if (printAfterRx) {
                        printAfterRx = false;
                        print();
                    }
                    deleteOnReceive = true;
                } else if (character == EOL) {
                    if (writingLine < LINE_COUNT - 1) writingLine++;
                    setChanged();
                } else if (character == BELL) {
                    level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.BLOCKS, 2.0F, 0.5F);
                } else if (character == PRINT) {
                    printAfterRx = true;
                } else if (character == CLEAR) {
                    for (int i = 0; i < LINE_COUNT; i++) rxBuffer[i] = "";
                    writingLine = 0;
                } else {
                    rxBuffer[writingLine] += character;
                    setChanged();
                }
            }
        }

        networkPackNT();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer);
        for (int i = 0; i < LINE_COUNT; i++) {
            buffer.writeUtf(value(txBuffer[i]));
            buffer.writeUtf(value(rxBuffer[i]));
        }
        buffer.writeUtf(value(txChannel));
        buffer.writeUtf(value(rxChannel));
        buffer.writeChar(sendingChar);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer);
        for (int i = 0; i < LINE_COUNT; i++) {
            txBuffer[i] = buffer.readUtf(LINE_WIDTH);
            // RX lines are appended from the radio stream and were unbounded in old-hbm.
            rxBuffer[i] = buffer.readUtf();
        }
        txChannel = buffer.readUtf(CHANNEL_LENGTH);
        rxChannel = buffer.readUtf(CHANNEL_LENGTH);
        sendingChar = buffer.readChar();
    }

    @Override
    public void receiveControl(CompoundTag data) {
        for (int i = 0; i < LINE_COUNT; i++) {
            String key = "tx" + i;
            if (data.contains(key)) txBuffer[i] = limit(Tags.getStringOr(data, key, ""), LINE_WIDTH);
        }

        String command = Tags.getStringOr(data, "cmd", "");
        if ("snd".equals(command) && !isSending) {
            isSending = true;
            sendingLine = 0;
            sendingIndex = 0;
        }
        if ("rxprt".equals(command)) print();
        if ("rxcls".equals(command)) clearReceiveBuffer();
        if ("sve".equals(command)) {
            txChannel = limit(Tags.getStringOr(data, "txChan", ""), CHANNEL_LENGTH);
            rxChannel = limit(Tags.getStringOr(data, "rxChan", ""), CHANNEL_LENGTH);
        }

        dataChanged();
        setChanged();
    }

    private void clearReceiveBuffer() {
        for (int i = 0; i < LINE_COUNT; i++) rxBuffer[i] = "";
        writingLine = 0;
        setChanged();
    }

    /** Prints every nonempty receive line as one paper message, exactly as the old machine did. */
    public void print() {
        if (level == null || level.isClientSide()) return;

        List<String> text = new ArrayList<>();
        for (int i = 0; i < LINE_COUNT; i++) {
            if (!rxBuffer[i].isEmpty()) text.add(rxBuffer[i]);
        }
        ItemStack stack = new ItemStack(Items.PAPER);
        ItemStacks.setNameAndLore(stack, "Message", text);
        ItemEntity item = new ItemEntity(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 1D,
                worldPosition.getZ() + 0.5D, stack);
        level.addFreshEntity(item);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) < 16D * 16D;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < LINE_COUNT; i++) {
            output.putString("tx" + i, value(txBuffer[i]));
            output.putString("rx" + i, value(rxBuffer[i]));
        }
        output.putString("txChan", value(txChannel));
        output.putString("rxChan", value(rxChannel));
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < LINE_COUNT; i++) {
            txBuffer[i] = limit(input.getStringOr("tx" + i, ""), LINE_WIDTH);
            rxBuffer[i] = limit(input.getStringOr("rx" + i, ""), LINE_WIDTH);
        }
        txChannel = limit(input.getStringOr("txChan", ""), CHANNEL_LENGTH);
        rxChannel = limit(input.getStringOr("rxChan", ""), CHANNEL_LENGTH);
    }

    @Override
    protected boolean syncFullTagOnChunkLoad() {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.radio_telex");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RadioTelexMenu(containerId, inventory, this);
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }

    private static String limit(String text, int max) {
        String value = text == null ? "" : text;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
