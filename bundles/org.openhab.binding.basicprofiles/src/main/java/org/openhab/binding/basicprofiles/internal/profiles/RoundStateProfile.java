/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.basicprofiles.internal.profiles;

import static org.openhab.binding.basicprofiles.internal.factory.BasicProfilesFactory.ROUND_UID;

import java.math.RoundingMode;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.basicprofiles.internal.config.RoundStateProfileConfig;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.thing.profiles.ProfileTypeUID;
import org.openhab.core.thing.profiles.StateProfile;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies rounding with the specified scale and the rounding mode to a {@link QuantityType} or {@link DecimalType}
 * state. Default rounding mode is {@link RoundingMode#HALF_UP}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class RoundStateProfile implements StateProfile {

    private final Logger logger = LoggerFactory.getLogger(RoundStateProfile.class);

    private final ProfileCallback callback;

    final int scale;
    final RoundingMode roundingMode;

    public RoundStateProfile(ProfileCallback callback, ProfileContext context) {
        this.callback = callback;
        RoundStateProfileConfig config = context.getConfiguration().as(RoundStateProfileConfig.class);
        logger.debug("Configuring profile with parameters: [{scale='{}', mode='{}']", config.scale, config.mode);

        int localScale = 0;
        // if (config.scale instanceof String) {
        // try {
        // scale = Integer.valueOf((String) config.scale);
        // } catch (NumberFormatException e) {
        // logger.error("Cannot convert value '{}' of parameter 'scale' into a valid integer.", config.scale);
        // }
        // } else
        if (config.scale instanceof Number) {
            localScale = ((Number) config.scale).intValue();
        } else {
            logger.error("Parameter 'scale' is not of type String or Number.");
        }

        if (localScale < 0) {
            logger.warn("Parameter 'scale' has to be a non-negative integer. Ignoring it.");
            localScale = 0;
        }

        RoundingMode localRoundingMode = RoundingMode.HALF_UP;
        if (config.mode instanceof String) {
            try {
                localRoundingMode = RoundingMode.valueOf(config.mode);
            } catch (IllegalArgumentException e) {
                logger.warn("Parameter 'mode' is not a supported rounding mode: '{}'. Using default.", config.mode);
            }
        } else {
            logger.error("Parameter 'mode' is not of type String.");
        }

        this.scale = localScale;
        this.roundingMode = localRoundingMode;
    }

    @Override
    public ProfileTypeUID getProfileTypeUID() {
        return ROUND_UID;
    }

    @Override
    public void onStateUpdateFromItem(State state) {
        callback.sendUpdate((State) applyRound(state));
    }

    @Override
    public void onCommandFromItem(Command command) {
        callback.handleCommand((Command) applyRound(command));
    }

    @Override
    public void onCommandFromHandler(Command command) {
        callback.sendCommand((Command) applyRound(command));
    }

    @Override
    public void onStateUpdateFromHandler(State state) {
        callback.sendUpdate((State) applyRound(state));
    }

    private Type applyRound(Type state) {
        if (state instanceof UnDefType) {
            // we cannot round UNDEF or NULL values, thus we simply return them without reporting an error or warning
            return state;
        }

        Type result = UnDefType.UNDEF;
        if (state instanceof QuantityType) {
            QuantityType<?> qtState = (QuantityType<?>) state;
            result = new QuantityType<>(qtState.toBigDecimal().setScale(scale, roundingMode), qtState.getUnit());
        } else if (state instanceof DecimalType) {
            DecimalType dtState = (DecimalType) state;
            result = new DecimalType(dtState.toBigDecimal().setScale(scale, roundingMode));
        } else {
            logger.warn(
                    "Round cannot be applied to the incompatible state '{}' sent from the binding. Returning original state.",
                    state);
            result = state;
        }
        return result;
    }
}