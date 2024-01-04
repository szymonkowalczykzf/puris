/*
 * Copyright (c) 2023, 2024 Volkswagen AG
 * Copyright (c) 2023, 2024 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e.V.
 * (represented by Fraunhofer ISST)
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.puris.backend.stock.logic.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.MaterialDto;
import org.eclipse.tractusx.puris.backend.stock.domain.model.datatype.DT_StockTypeEnum;
import org.eclipse.tractusx.puris.backend.stock.domain.model.measurement.MeasurementUnit;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.samm.LocationIdTypeEnum;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@ToString
public abstract class StockDto implements Serializable {

    private UUID uuid;

    private MaterialDto material;

    private double quantity;

    private MeasurementUnit measurementUnit;

    private String stockLocationBpns;

    private String stockLocationBpna;

    private LocationIdTypeEnum locationIdType;

    private DT_StockTypeEnum type;

    private Date lastUpdatedOn;

    public StockDto(MaterialDto material, double quantity, MeasurementUnit measurementUnit, String stockLocationBpns,
                    String stockLocationBpna, Date lastUpdatedOn) {
        this.material = material;
        this.quantity = quantity;
        this.measurementUnit = measurementUnit;
        this.stockLocationBpns = stockLocationBpns;
        this.stockLocationBpna = stockLocationBpna;
        this.lastUpdatedOn = lastUpdatedOn;
    }
}
