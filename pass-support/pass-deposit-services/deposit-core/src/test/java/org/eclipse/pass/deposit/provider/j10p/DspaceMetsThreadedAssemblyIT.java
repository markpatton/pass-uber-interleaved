/*
 * Copyright 2019 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.pass.deposit.provider.j10p;

import static java.util.Collections.singletonList;

import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.pass.deposit.assembler.PackageOptions.Archive;
import org.eclipse.pass.deposit.assembler.PackageOptions.Checksum;
import org.eclipse.pass.deposit.assembler.PackageOptions.Compression;
import org.eclipse.pass.deposit.assembler.PackageOptions.Spec;
import org.eclipse.pass.deposit.assembler.AbstractAssembler;
import org.eclipse.pass.deposit.assembler.PackageVerifier;
import org.eclipse.pass.deposit.assembler.ThreadedAssemblyIT;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DspaceMetsThreadedAssemblyIT extends ThreadedAssemblyIT {

    @Override
    protected AbstractAssembler assemblerUnderTest() {
        DspaceMetadataDomWriterFactory metsWriterFactory =
            new DspaceMetadataDomWriterFactory(DocumentBuilderFactory.newInstance());
        DspaceMetsPackageProviderFactory ppf = new DspaceMetsPackageProviderFactory(metsWriterFactory);
        return new DspaceMetsAssembler(mbf, rbf, ppf);
    }

    @Override
    protected Map<String, Object> packageOptions() {
        return new HashMap<>() {
            {
                put(Spec.KEY, DspaceMetsAssembler.SPEC_DSPACE_METS);
                put(Archive.KEY, Archive.OPTS.ZIP);
                put(Compression.KEY, Compression.OPTS.ZIP);
                put(Checksum.KEY, singletonList(Checksum.OPTS.SHA256));
            }
        };
    }

    @Override
    protected PackageVerifier packageVerifier() {
        return new DspaceMetsPackageVerifier();
    }

}
