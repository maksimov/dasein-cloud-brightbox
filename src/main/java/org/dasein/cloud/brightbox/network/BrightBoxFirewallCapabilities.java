/*
 * Copyright (C) 2009-2015 Dell, Inc.
 * See annotations for authorship information
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.brightbox.network;

import org.dasein.cloud.AbstractCapabilities;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.Requirement;
import org.dasein.cloud.VisibleScope;
import org.dasein.cloud.brightbox.BrightBoxCloud;
import org.dasein.cloud.network.Direction;
import org.dasein.cloud.network.FirewallCapabilities;
import org.dasein.cloud.network.FirewallConstraints;
import org.dasein.cloud.network.Permission;
import org.dasein.cloud.network.Protocol;
import org.dasein.cloud.network.RuleTargetType;
import org.dasein.cloud.util.NamingConstraints;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by stas on 20/02/2015.
 */
public class BrightBoxFirewallCapabilities extends AbstractCapabilities<BrightBoxCloud> implements FirewallCapabilities {

    public BrightBoxFirewallCapabilities(BrightBoxCloud provider) {
        super(provider);
    }

    @Override
    public @Nonnull FirewallConstraints getFirewallConstraintsForCloud() throws InternalException, CloudException {
        return FirewallConstraints.getInstance();
    }

    @Override
    public @Nonnull String getProviderTermForFirewall(@Nonnull Locale locale) {
        return "firewall policy";
    }

    @Override
    public @Nullable VisibleScope getFirewallVisibleScope() {
        return null;
    }

    @Override
    public @Nonnull Requirement identifyPrecedenceRequirement(boolean inVlan) throws InternalException, CloudException {
        return Requirement.NONE;
    }

    @Override
    public boolean isZeroPrecedenceHighest() throws InternalException, CloudException {
        return false;
    }

    @Override
    @Deprecated
    public @Nonnull Iterable<RuleTargetType> listSupportedDestinationTypes(boolean inVlan) throws InternalException, CloudException {
        throw new RuntimeException("listSupportedDestinationTypes(boolean) is deprecated");
    }

    @Override
    public @Nonnull Iterable<RuleTargetType> listSupportedDestinationTypes(boolean inVlan, Direction direction) throws InternalException, CloudException {
        if( Direction.EGRESS.equals(direction) && !inVlan ) {
            return Arrays.asList(RuleTargetType.GLOBAL, RuleTargetType.CIDR, RuleTargetType.VM);
        }
        return Collections.emptyList();
    }

    private volatile transient List<Direction> directions;

    @Override
    public @Nonnull Iterable<Direction> listSupportedDirections(boolean inVlan) throws InternalException, CloudException {
        if( inVlan ) {
            return Collections.emptyList();
        }
        if( directions == null ) {
            directions = Collections.unmodifiableList(Arrays.asList(Direction.INGRESS, Direction.EGRESS));
        }
        return directions;
    }

    private volatile transient List<Permission> permissions;

    @Override
    public @Nonnull Iterable<Permission> listSupportedPermissions(boolean inVlan) throws InternalException, CloudException {
        if( inVlan ) {
            return Collections.emptyList();
        }
        if( permissions == null ) {
            permissions = Collections.unmodifiableList(Collections.singletonList(Permission.ALLOW));
        }
        return permissions;
    }

    private volatile transient List<Protocol> protocols;

    @Override
    public @Nonnull Iterable<Protocol> listSupportedProtocols(boolean inVlan) throws InternalException, CloudException {
        if( inVlan ) {
            return Collections.emptyList();
        }
        if( protocols == null ) {
            protocols = Collections.unmodifiableList(Arrays.asList(Protocol.TCP, Protocol.ICMP, Protocol.UDP));
        }
        return protocols;
    }

    private volatile transient List<RuleTargetType> sourceTypes;

    @Override
    @Deprecated
    public @Nonnull Iterable<RuleTargetType> listSupportedSourceTypes(boolean inVlan) throws InternalException, CloudException {
        throw new RuntimeException("listSupportedSourceTypes(boolean) is deprecated");
    }

    @Override
    public @Nonnull Iterable<RuleTargetType> listSupportedSourceTypes(boolean inVlan, Direction direction) throws InternalException, CloudException {
        if( Direction.INGRESS.equals(direction) && !inVlan ) {
            return Arrays.asList(RuleTargetType.GLOBAL, RuleTargetType.CIDR, RuleTargetType.VM);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean requiresRulesOnCreation() throws CloudException, InternalException {
        return false;
    }

    @Override
    public Requirement requiresVLAN() throws CloudException, InternalException {
        return Requirement.NONE;
    }

    @Override
    public boolean supportsRules(@Nonnull Direction direction, @Nonnull Permission permission, boolean inVlan) throws CloudException, InternalException {
        return !inVlan && Permission.ALLOW.equals(permission);
    }

    @Override
    public boolean supportsFirewallCreation(boolean inVlan) throws CloudException, InternalException {
        return !inVlan;
    }

    @Override
    public boolean supportsFirewallDeletion() throws CloudException, InternalException {
        return true;
    }

    @Nonnull
    @Override
    public NamingConstraints getFirewallNamingConstraints() throws CloudException, InternalException {
        return NamingConstraints.getAlphaNumeric(0, 255);
    }
}
