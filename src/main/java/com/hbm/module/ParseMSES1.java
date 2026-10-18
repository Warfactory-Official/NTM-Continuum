package com.hbm.module;

import com.hbm.config.ServerConfig;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.tileentity.network.RTTYSystem.RTTYChannel;

import java.util.Locale;

public class ParseMSES1 implements IParse {

    @Override
    public EnumStatementReturn eval(ParseContext ctx, String line) {
        String lower = line.toLowerCase(Locale.US);

        // jump point destination or comment, skip
        if (line.isEmpty() || lower.startsWith("dest ") || lower.startsWith("# ")) {
            return EnumStatementReturn.SKIP;
        }

        // no operation, still eats up a clock cycle
        if (lower.equals("nop")) {
            return EnumStatementReturn.OK;
        }

        // sets the desired clock speed, then skips the operation
        if (lower.startsWith("clockspeed ")) {
            if (line.length() <= 11) return EnumStatementReturn.PARAMETER_MISSING;
            try {
                int speed = Integer.parseInt(line.substring(11));
                if (speed < 1 || speed > ServerConfig.AUTOCAL_MAX_CLOCK.get()) return EnumStatementReturn.PARAMETER_OOB;
                ctx.clockSpeed = speed;
            } catch (NumberFormatException ex) {
                return EnumStatementReturn.PARAMETER_PARSE_ERROR;
            }
            return EnumStatementReturn.SKIP;
        }

        // sets the script index to the jump point
        if (lower.startsWith("jmp ")) {
            if (line.length() <= 4) return EnumStatementReturn.PARAMETER_MISSING;
            return jump(ctx, line.substring(4));
        }

        // sets the script index to the jump point, if the buffer is the string 'true'
        if (lower.startsWith("jmpif ")) {
            if (line.length() <= 6) return EnumStatementReturn.PARAMETER_MISSING;
            if (!ctx.readBuffer().equals("true")) return EnumStatementReturn.OK;
            return jump(ctx, line.substring(6));
        }

        // sets the script index to the jump point, if the buffer is the NOT 'true'
        if (lower.startsWith("jmpnot ")) {
            if (line.length() <= 7) return EnumStatementReturn.PARAMETER_MISSING;
            if (ctx.readBuffer().equals("true")) return EnumStatementReturn.OK;
            return jump(ctx, line.substring(7));
        }

        // ends the tick regardless of remaining clock cycles
        if (lower.equals("endtick")) {
            return EnumStatementReturn.END_TICK;
        }

        // requests unit to shut down
        if (lower.equals("shutdown")) {
            return EnumStatementReturn.SHUTDOWN;
        }

        // loads the requested variable into the buffer
        if (lower.startsWith("load ")) {
            if (line.length() <= 5) return EnumStatementReturn.PARAMETER_MISSING;
            ctx.writeBuffer(ctx.getVariable(line.substring(5)));
            return EnumStatementReturn.OK;
        }

        // saves the buffer with the specified name
        if (lower.startsWith("save ")) {
            if (line.length() <= 5) return EnumStatementReturn.PARAMETER_MISSING;
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.BUFFER_EMPTY;
            ctx.setVariable(line.substring(5), ctx.readBuffer());
            return EnumStatementReturn.OK;
        }

        // stores the specified value in the buffer
        if (lower.startsWith("buffer ")) {
            if (line.length() <= 7) return EnumStatementReturn.PARAMETER_MISSING;
            ctx.writeBuffer(line.substring(7));
            return EnumStatementReturn.OK;
        }

        // runs the calculation, allows string substitution, saves result to buffer
        if (lower.startsWith("eval ")) {
            if (line.length() <= 5) return EnumStatementReturn.PARAMETER_MISSING;
            return calculate(ctx, substitute(ctx, line.substring(5), true), false);
        }

        // runs the calculation from the buffer, allows string substitution, saves result to buffer
        if (lower.equals("eval")) {
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.BUFFER_EMPTY;
            return calculate(ctx, substitute(ctx, ctx.readBuffer(), true), false);
        }

        // runs the calculation, allows string substitution, rounds, saves result to buffer
        if (lower.startsWith("evalr ")) {
            if (line.length() <= 6) return EnumStatementReturn.PARAMETER_MISSING;
            return calculate(ctx, substitute(ctx, line.substring(6), true), true);
        }

        // runs the calculation from the buffer, allows string substitution, saves rounded result to buffer
        if (lower.equals("evalr")) {
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.BUFFER_EMPTY;
            return calculate(ctx, substitute(ctx, ctx.readBuffer(), true), true);
        }

        // rounds the buffer down to the nearest integer
        if (lower.equals("rounddown") || lower.equals("floor")) {
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.BUFFER_EMPTY;
            try {
                ctx.writeBuffer("" + (int) Math.floor(Double.parseDouble(ctx.readBuffer())));
            } catch (NumberFormatException ex) {
                return EnumStatementReturn.ARITHMETIC_EXCEPTION;
            }
            return EnumStatementReturn.OK;
        }

        // rounds the buffer up to the nearest integer
        if (lower.equals("roundup") || lower.equals("ceil")) {
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.BUFFER_EMPTY;
            try {
                ctx.writeBuffer("" + (int) Math.ceil(Double.parseDouble(ctx.readBuffer())));
            } catch (NumberFormatException ex) {
                return EnumStatementReturn.ARITHMETIC_EXCEPTION;
            }
            return EnumStatementReturn.OK;
        }

        // rounds the buffer to the nearest integer (.5 cutoff rule)
        if (lower.equals("round") || lower.equals("nearest")) {
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.BUFFER_EMPTY;
            try {
                ctx.writeBuffer("" + (int) Math.round(Double.parseDouble(ctx.readBuffer())));
            } catch (NumberFormatException ex) {
                return EnumStatementReturn.ARITHMETIC_EXCEPTION;
            }
            return EnumStatementReturn.OK;
        }

        // concatenate, same as buffer but evaluates $var$ substitutions
        if (lower.startsWith("concat ")) {
            if (line.length() <= 7) return EnumStatementReturn.PARAMETER_MISSING;
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.BUFFER_EMPTY;
            ctx.writeBuffer(substitute(ctx, line.substring(7), false));
            return EnumStatementReturn.OK;
        }

        // compares the buffer with a value, allows substitutions
        if (lower.startsWith("eq ")) {
            if (line.length() <= 3) return EnumStatementReturn.PARAMETER_MISSING;
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.BUFFER_EMPTY;
            ctx.writeBuffer(ctx.readBuffer().equals(substitute(ctx, line.substring(3), false)) ? "true" : "false");
            return EnumStatementReturn.OK;
        }

        // greater than, lower than, greater or equal, lower or equal buffer
        if (lower.startsWith("gtb ") || lower.startsWith("ltb ") || lower.startsWith("geb ") || lower.startsWith("leb ")) {
            if (line.length() <= 4) return EnumStatementReturn.PARAMETER_MISSING;
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.BUFFER_EMPTY;
            try {
                double buffer = Double.parseDouble(ctx.readBuffer());
                double val = Double.parseDouble(substitute(ctx, line.substring(4), false));
                boolean result = switch (lower.substring(0, 3)) {
                    case "gtb" -> val > buffer;
                    case "ltb" -> val < buffer;
                    case "geb" -> val >= buffer;
                    default -> val <= buffer;
                };
                ctx.writeBuffer(result ? "true" : "false");
            } catch (NumberFormatException ex) {
                return EnumStatementReturn.ARITHMETIC_EXCEPTION;
            }
            return EnumStatementReturn.OK;
        }

        // sends an RoR signal using the buffer's contents as the message over the supplied channel
        if (lower.startsWith("send ")) {
            if (line.length() <= 5) return EnumStatementReturn.PARAMETER_MISSING;
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.BUFFER_EMPTY;
            RTTYSystem.broadcast(ctx.world, substitute(ctx, line.substring(5), false), ctx.readBuffer());
            return EnumStatementReturn.OK;
        }

        // listens to an RoR signal using the supplied channel name and saves it to the buffer
        if (lower.startsWith("listen ")) {
            if (line.length() <= 7) return EnumStatementReturn.PARAMETER_MISSING;
            RTTYChannel chan = RTTYSystem.listen(ctx.world, substitute(ctx, line.substring(7), false));
            if (chan != null) ctx.writeBuffer(chan.signal + "");
            return EnumStatementReturn.OK;
        }

        return EnumStatementReturn.UNRECOGNIZED_COMMAND;
    }

    private EnumStatementReturn jump(ParseContext ctx, String key) {
        String jmpKey = substitute(ctx, key, false);
        if (!ctx.jmp.containsKey(jmpKey)) return EnumStatementReturn.NO_DESTINATION;
        ctx.current = ctx.jmp.get(jmpKey);
        return EnumStatementReturn.OK;
    }

    private static EnumStatementReturn calculate(ParseContext ctx, String statement, boolean round) {
        try {
            double result = Calculator.evaluateExpression(statement);
            ctx.writeBuffer(round ? "" + (int) Math.round(result) : "" + result);
        } catch (RuntimeException ex) {
            // a malformed expression throws IllegalArgumentException or an index/number exception
            return EnumStatementReturn.ARITHMETIC_EXCEPTION;
        }
        return EnumStatementReturn.OK;
    }

    /** Replaces each {@code $name$} with the variable or the buffer; an unclosed {@code $} drops the rest. */
    public String substitute(ParseContext ctx, String statement, boolean forceNumber) {
        if (!statement.contains("$")) return statement;

        StringBuilder joined = new StringBuilder();
        StringBuilder var = new StringBuilder();
        boolean readingVar = false;

        for (char c : statement.toCharArray()) {
            if (c == '$') {
                if (!readingVar) {
                    readingVar = true;
                } else {
                    String varName = var.toString();
                    String variable = "buffer".equals(varName) ? ctx.readBuffer() : ctx.getVariable(varName);
                    if (forceNumber && variable.isEmpty()) variable = "0";
                    joined.append(variable);
                    var.setLength(0);
                    readingVar = false;
                }
            } else if (readingVar) {
                var.append(c);
            } else {
                joined.append(c);
            }
        }

        return joined.toString();
    }

    @Override
    public void generateJumpPoints(ParseContext ctx, String line, int index) {
        if (line.startsWith("dest ") && line.length() > 5) {
            ctx.jmp.put(line.substring(5), index);
        }
    }
}
