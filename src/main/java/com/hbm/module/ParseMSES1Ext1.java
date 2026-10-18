package com.hbm.module;

import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.tileentity.network.RTTYSystem.RTTYChannel;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * MSES1-FEIS: Machine Script, Equestrian Standard - First Extended Instruction Set (v1.1)
 *
 * Additions compared to v1 Standard:
 * * Support for splitter chars, splitting and split count
 * * Support for the stack, a secondary buffer that can push, pop and peek
 * * Support for getting string length and substring using first and last
 * * Support for listening only to recent RoR signals using poll
 *
 * @author hbm
 */
public class ParseMSES1Ext1 extends ParseMSES1 {

    @Override
    public EnumStatementReturn eval(ParseContext ctx, String line) {
        String lower = line.toLowerCase(Locale.US);

        // sets the splitter char
        if (lower.startsWith("splitter ")) {
            if (line.length() <= 9) return EnumStatementReturn.PARAMETER_MISSING;
            ctx.splitString = substitute(ctx, line.substring(9), false);
            return EnumStatementReturn.OK;
        }

        // grabs a fragment based on index
        if (lower.startsWith("split ")) {
            if (line.length() <= 6) return EnumStatementReturn.PARAMETER_MISSING;
            try {
                int index = Integer.parseInt(substitute(ctx, line.substring(6), true));
                if (index < 1) return EnumStatementReturn.PARAMETER_OOB;
                String[] frags = ctx.readBuffer().split(Pattern.quote(ctx.splitString));
                if (index > frags.length) return EnumStatementReturn.PARAMETER_OOB;
                ctx.writeBuffer(frags[index - 1]);
                return EnumStatementReturn.OK;
            } catch (NumberFormatException ex) {
                return EnumStatementReturn.PARAMETER_PARSE_ERROR;
            }
        }

        // counts the amount of fragments in this string
        if (lower.equals("splitcount")) {
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.BUFFER_EMPTY;
            ctx.writeBuffer(ctx.readBuffer().split(Pattern.quote(ctx.splitString)).length + "");
            return EnumStatementReturn.OK;
        }

        // pushes the buffer to stack
        if (lower.equals("push")) {
            if (ctx.readBuffer().isEmpty()) return EnumStatementReturn.BUFFER_EMPTY;
            return ctx.push(ctx.readBuffer()) ? EnumStatementReturn.OK : EnumStatementReturn.STACK_EXCEEDED;
        }

        // pushes the supplied value (or variable) to stack
        if (lower.startsWith("push ")) {
            if (line.length() <= 5) return EnumStatementReturn.PARAMETER_MISSING;
            return ctx.push(substitute(ctx, line.substring(5), false)) ? EnumStatementReturn.OK : EnumStatementReturn.STACK_EXCEEDED;
        }

        // removes the most recent element from stack and writes it to the buffer
        if (lower.equals("pop")) {
            String val = ctx.pop();
            if (val == null) return EnumStatementReturn.UNDEFINED;
            ctx.writeBuffer(val);
            return EnumStatementReturn.OK;
        }

        // writes the most recent element to the buffer
        if (lower.equals("peek")) {
            String val = ctx.peek();
            if (val == null) return EnumStatementReturn.UNDEFINED;
            ctx.writeBuffer(val);
            return EnumStatementReturn.OK;
        }

        // figures out buffer string's length
        if (lower.equals("length")) {
            ctx.writeBuffer("" + ctx.readBuffer().length());
            return EnumStatementReturn.OK;
        }

        // grabs the first x characters from the buffer and writes them back to the buffer
        if (lower.startsWith("first ")) {
            if (line.length() <= 6) return EnumStatementReturn.PARAMETER_MISSING;
            try {
                int length = Math.min(Integer.parseInt(substitute(ctx, line.substring(6), true)), ctx.readBuffer().length());
                ctx.writeBuffer(ctx.readBuffer().substring(0, length));
                return EnumStatementReturn.OK;
            } catch (NumberFormatException | IndexOutOfBoundsException ex) {
                return EnumStatementReturn.PARAMETER_PARSE_ERROR;
            }
        }

        // grabs the last x characters from the buffer and writes them back to the buffer
        if (lower.startsWith("last ")) {
            if (line.length() <= 5) return EnumStatementReturn.PARAMETER_MISSING;
            try {
                int max = ctx.readBuffer().length();
                int length = Math.min(Integer.parseInt(substitute(ctx, line.substring(5), true)), max);
                ctx.writeBuffer(ctx.readBuffer().substring(max - length, max));
                return EnumStatementReturn.OK;
            } catch (NumberFormatException | IndexOutOfBoundsException ex) {
                return EnumStatementReturn.PARAMETER_PARSE_ERROR;
            }
        }

        // listens to an RoR signal using the supplied channel name and saves it to the buffer
        if (lower.startsWith("poll ")) {
            if (line.length() <= 5) return EnumStatementReturn.PARAMETER_MISSING;
            RTTYChannel chan = RTTYSystem.listen(ctx.world, substitute(ctx, line.substring(5), false));
            if (chan != null && chan.timeStamp >= ctx.world.getGameTime() - 1) ctx.writeBuffer(chan.signal + "");
            return EnumStatementReturn.OK;
        }

        // writes the total world time to the buffer
        if (lower.equals("worldtime")) {
            ctx.writeBuffer("" + ctx.world.getGameTime());
            return EnumStatementReturn.OK;
        }

        return super.eval(ctx, line);
    }
}
