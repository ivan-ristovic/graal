/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.svm.hosted;

import com.oracle.graal.pointsto.meta.AnalysisUniverse;
import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.jdk.BootModuleLayerSupport;
import com.oracle.svm.core.jdk.JDK11OrLater;
import com.oracle.svm.util.ReflectionUtil;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import java.util.Set;
import java.util.stream.Collectors;

@AutomaticFeature
@Platforms(Platform.HOSTED_ONLY.class)
public final class ModuleLayerFeature implements Feature {

    @Override
    public boolean isInConfiguration(IsInConfigurationAccess access) {
        return new JDK11OrLater().getAsBoolean();
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        access.registerReachabilityHandler(
                a -> a.registerAsUnsafeAccessed(ReflectionUtil.lookupField(ClassLoader.class, "classLoaderValueMap")),
                ReflectionUtil.lookupMethod(ClassLoader.class, "trySetObjectField", String.class, Object.class)
        );
        RuntimeReflection.register(ReflectionUtil.lookupField(ClassLoader.class, "classLoaderValueMap"));
        ImageSingletons.add(BootModuleLayerSupport.class, new BootModuleLayerSupport());
    }

    @Override
    public void afterAnalysis(AfterAnalysisAccess access) {
        FeatureImpl.AfterAnalysisAccessImpl accessImpl = (FeatureImpl.AfterAnalysisAccessImpl) access;
        AnalysisUniverse universe = accessImpl.getUniverse();

        Set<Object> moduleSet = universe.getTypes()
                .stream()
                .filter(t -> t.isReachable() && !t.isArray())
                .map(t -> t.getJavaClass().getModule())
                .filter(Module::isNamed)
                .collect(Collectors.toSet());

        BootModuleLayerSupport.instance().setReachableModules(moduleSet);
    }
}
