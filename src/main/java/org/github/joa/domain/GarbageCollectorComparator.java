/**********************************************************************************************************************
 * JVM Options Analyzer                                                                                               *
 *                                                                                                                    *
 * Copyright (c) 2022-2023 Mike Millson                                                                                    *
 *                                                                                                                    *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License       *
 * v. 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0 which is    *
 * available at https://www.apache.org/licenses/LICENSE-2.0.                                                          *
 *                                                                                                                    *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0                                                                     *
 *                                                                                                                    *
 * Contributors:                                                                                                      *
 *    Mike Millson - initial API and implementation                                                                   *
 *********************************************************************************************************************/
package org.github.joa.domain;

import java.util.Comparator;

/**
 * <p>
 * Defined garbage <code>Comparator</code>.
 * </p>
 * 
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 */
public class GarbageCollectorComparator implements Comparator<GarbageCollector> {
    @Override
    public int compare(GarbageCollector collector1, GarbageCollector collector2) {
        return collector1.toString().compareTo(collector2.toString());
    }
}
