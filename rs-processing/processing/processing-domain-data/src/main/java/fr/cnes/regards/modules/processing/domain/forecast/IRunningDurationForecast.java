/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
*/
package fr.cnes.regards.modules.processing.domain.forecast;

import io.vavr.control.Try;

import java.time.Duration;

/**
 * This interface defines the signature for a size forecast: how to estimate the amount of disk usage
 * a execution will generate.
 *
 * @author gandrieu
 */
public interface IRunningDurationForecast {

    Duration expectedRunningDurationInBytes(long inputSizeInBytes);

    static IRunningDurationForecast defaultDuration() { return i -> Duration.ofDays(1); }

    static IRunningDurationForecast secondsPerMegabytes(int secondsPerMegabyte) {
        return i -> Duration.ofSeconds(secondsPerMegabyte * (i / (1024L*1024L)));
    }

    static IRunningDurationForecast constant(int seconds) {
        return i -> Duration.ofSeconds(seconds);
    }

    interface Parser {
        Try<IRunningDurationForecast> parseRunningDurationForecast(String str);
    }
}
