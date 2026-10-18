package com.hbm.module;

import dev.golem.core.data.Tags;

import com.hbm.config.ServerConfig;
import com.mojang.serialization.Codec;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IParse {
    EnumStatementReturn eval(ParseContext ctx, String line);
    void generateJumpPoints(ParseContext ctx, String line, int index);

    class ParseContext {
        public static final Codec<List<String>> SCRIPT_CODEC = Codec.STRING.listOf();
        public static final Codec<Map<String, String>> VARIABLES_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING);

        public Level world;
        public final Map<String, String> variables = new HashMap<>();
        public final HashMap<String, Integer> jmp = new HashMap<>();

        private String buffer = "";
        private final String[] stack = new String[ServerConfig.MAX_STACK_SIZE.get()];
        private int stackSize = 0;
        public String splitString = ";";
        public int clockSpeed = 1;
        public int current = 0;

        public ParseContext(Level world) {
            this.world = world;
            Arrays.fill(this.stack, "");
        }

        public String readBuffer() {
            return this.buffer;
        }

        /** Sets the buffer and imposes the {@code MAX_BUFFER_LENGTH} restriction; false if it truncated. */
        public boolean writeBuffer(String buffer) {
            int max = ServerConfig.MAX_BUFFER_LENGTH.get();
            if (buffer.length() > max) {
                this.buffer = buffer.substring(0, max);
                return false;
            }
            this.buffer = buffer;
            return true;
        }

        public boolean push(String line) {
            if (stackSize >= stack.length) return false;
            int max = ServerConfig.MAX_BUFFER_LENGTH.get();
            if (line.length() > max) line = line.substring(0, max);
            stack[stackSize] = line;
            stackSize++;
            return true;
        }

        public String pop() {
            if (stackSize <= 0) return null;
            if (stackSize > stack.length) stackSize = stack.length;
            String ret = stack[stackSize - 1];
            stack[stackSize - 1] = "";
            stackSize--;
            return ret;
        }

        public String peek() {
            if (stackSize <= 0) return null;
            if (stackSize > stack.length) stackSize = stack.length;
            return stack[stackSize - 1];
        }

        public String getVariable(String name) {
            return variables.getOrDefault(name, "");
        }

        public void setVariable(String name, String value) {
            variables.put(name, value);
        }

        public void turnOff() {
            this.clockSpeed = 1;
            this.current = 0;
            this.buffer = "";
            if (!this.variables.isEmpty()) this.variables.clear();
        }

        public void readData(DataIn input, String[] script, IParse parser) {
            current = input.getIntOr("current", 0);
            clockSpeed = input.getIntOr("clockSpeed", 0);
            buffer = input.getStringOr("buffer", "");
            splitString = input.getStringOr("splitString", "");
            variables.clear();
            input.read("variables", VARIABLES_CODEC).ifPresent(variables::putAll);

            stackSize = Math.max(0, Math.min(stack.length, input.getIntOr("stackSize", 0)));
            for (int i = 0; i < stackSize; i++) stack[i] = input.getStringOr("st" + i, "");
            jmp.clear();
            for (int i = 0; i < script.length; i++) parser.generateJumpPoints(this, script[i], i);
        }

        public void writeData(DataOut output) {
            output.putInt("current", current);
            output.putInt("clockSpeed", clockSpeed);
            output.putString("buffer", buffer);
            output.putString("splitString", splitString);
            output.store("variables", VARIABLES_CODEC, variables);

            output.putInt("stackSize", stackSize);
            for (int i = 0; i < stackSize; i++) output.putString("st" + i, stack[i]);
        }
    }

    enum EnumStatementReturn {
        OK(false),
        UNRECOGNIZED_COMMAND(true),
        /** Ends the tick regardless of remaining clock cycles. */
        END_TICK(false),
        SHUTDOWN(false),
        /** Skips the instruction without using up a clock cycle. */
        SKIP(false),
        UNDEFINED(true),
        STACK_EXCEEDED(true),
        PARAMETER_MISSING(true),
        PARAMETER_OOB(true),
        PARAMETER_PARSE_ERROR(true),
        /** Jump to an undefined jump point. */
        NO_DESTINATION(true),
        ARITHMETIC_EXCEPTION(true),
        BUFFER_EMPTY(true);

        public final boolean error;

        EnumStatementReturn(boolean error) {
            this.error = error;
        }
    }
}
