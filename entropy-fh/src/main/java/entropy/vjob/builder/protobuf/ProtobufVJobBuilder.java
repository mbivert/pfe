/*
 * Copyright (c) Fabien Hermenier
 *
 *        This file is part of Entropy.
 *
 *        Entropy is free software: you can redistribute it and/or modify
 *        it under the terms of the GNU Lesser General Public License as published by
 *        the Free Software Foundation, either version 3 of the License, or
 *        (at your option) any later version.
 *
 *        Entropy is distributed in the hope that it will be useful,
 *        but WITHOUT ANY WARRANTY; without even the implied warranty of
 *        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *        GNU Lesser General Public License for more details.
 *
 *        You should have received a copy of the GNU Lesser General Public License
 *        along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
 */

package entropy.vjob.builder.protobuf;

import entropy.configuration.Configuration;
import entropy.configuration.SimpleConfiguration;
import entropy.configuration.VirtualMachine;
import entropy.vjob.DefaultVJob;
import entropy.vjob.VJob;
import entropy.vjob.builder.VJobBuilder;
import entropy.vjob.builder.VJobBuilderException;
import entropy.vjob.builder.VJobElementBuilder;
import gnu.trove.THashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @author Fabien Hermenier
 */
public class ProtobufVJobBuilder implements VJobBuilder {

    private PBConstraintsCatalog catalog = emptyCatalog;

    private static final Configuration emptyConfiguratin = new SimpleConfiguration();

    private static final PBConstraintsCatalog emptyCatalog = new DefaultPBConstraintsCatalog();

    //private Configuration cfg = emptyConfiguratin;

    private VJobElementBuilder eBuilder;

    public ProtobufVJobBuilder(VJobElementBuilder eB) {
        this.eBuilder = eB;
    }

    public void setConstraintCatalog(PBConstraintsCatalog c) {
        this.catalog = c;
    }

    @Override
    public VJob build(File f) throws IOException, VJobBuilderException {
        try {
            PBVJob.vjob pbV = PBVJob.vjob.parseFrom(new FileInputStream(f));
            VJob v = new DefaultVJob(pbV.getId());
            for (PBVJob.vjob.VMDecl pbVm : pbV.getVmList()) {
                String tpl = pbVm.getTplName();
                String id = pbVm.getId();
                Map<String, String> options = new THashMap<String, String>();
                for (PBVJob.vjob.Option opt : pbVm.getOptionList()) {
                    String k = opt.hasValue() ? opt.getValue() : null;
                    options.put(opt.getId(), k);
                }
                VirtualMachine vm = eBuilder.matchVirtualMachine(id, tpl, options);
                if (vm == null) {
                    throw new VJobBuilderException("Unable to declare virtual machine '" + id);
                }
                v.addVirtualMachine(vm);
            }

            for (PBVJob.vjob.Constraint pbc : pbV.getConstraintList()) {
                PBPlacementConstraintBuilder b = catalog.getConstraint(pbc.getId());
                if (b == null) {
                    throw new VJobBuilderException("Constraint '" + pbc.getId() + "' unknown");
                }
                v.addConstraint(b.buildConstraint(eBuilder, pbc.getParamList()));
            }
            return v;
        } catch (ConstraintBuilderException e) {
            throw new VJobBuilderException(e.getMessage(), e);
        } catch (IOException e) {
            throw new VJobBuilderException(e.getMessage(), e);
        }
    }

    @Override
    public String getAssociatedExtension() {
        return "pbd";
    }

    @Override
    public VJobElementBuilder getElementBuilder() {
        return this.eBuilder;
    }

    @Override
    public void setElementBuilder(VJobElementBuilder eb) {
        this.eBuilder = eb;
    }
}
