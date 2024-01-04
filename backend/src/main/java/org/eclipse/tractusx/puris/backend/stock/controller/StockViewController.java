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
package org.eclipse.tractusx.puris.backend.stock.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.puris.backend.common.api.logic.service.VariablesService;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Material;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Partner;
import org.eclipse.tractusx.puris.backend.masterdata.domain.model.Site;
import org.eclipse.tractusx.puris.backend.masterdata.logic.dto.PartnerDto;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialPartnerRelationService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.MaterialService;
import org.eclipse.tractusx.puris.backend.masterdata.logic.service.PartnerService;
import org.eclipse.tractusx.puris.backend.stock.domain.model.MaterialStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.PartnerProductStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.ProductStock;
import org.eclipse.tractusx.puris.backend.stock.domain.model.Stock;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.*;
import org.eclipse.tractusx.puris.backend.stock.logic.dto.samm.LocationIdTypeEnum;
import org.eclipse.tractusx.puris.backend.stock.logic.service.MaterialStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.PartnerProductStockService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockRequestApiService;
import org.eclipse.tractusx.puris.backend.stock.logic.service.ProductStockService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class contains a range of REST endpoints to provide the frontend
 * the necessary data concerning stocks.
 */
@RestController
@RequestMapping("stockView")
@Slf4j
public class StockViewController {

    @Autowired
    private ProductStockService productStockService;

    @Autowired
    private MaterialStockService materialStockService;

    @Autowired
    private PartnerProductStockService partnerProductStockService;

    @Autowired
    private ProductStockRequestApiService productStockRequestApiService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private MaterialPartnerRelationService mprService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private VariablesService variablesService;

    private final Pattern materialPattern = Pattern.compile(Material.MATERIAL_NUMBER_REGEX);

    @GetMapping("materials")
    @ResponseBody
    @Operation(description = "Returns a list of all materials (excluding products)")
    public List<FrontendMaterialDto> getMaterials() {
        return materialService.findAllMaterials()
            .stream()
            .map(mat -> new FrontendMaterialDto(mat.getOwnMaterialNumber(), mat.getName()))
            .collect(Collectors.toList());
    }

    @GetMapping("materialnumbers-mapping")
    @Operation(description = "Returns a mapping of all material numbers, that others partners are using" +
        "for the material given in the request parameter.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", content = @Content(examples = {
            @ExampleObject(name = "Basic sample", value = "{" +
                "  \"BPNL1234567890ZZ\": \"MNR-8101-ID146955.001\"," +
                "  \"BPNL4444444444XX\": \"MNR-7307-AU340474.002\"}")})),
        @ApiResponse(responseCode = "400", description = "Invalid parameter")})
    public ResponseEntity<Map<String, String>> getMaterialNumbers(@RequestParam String ownMaterialNumber) {
        if (!materialPattern.matcher(ownMaterialNumber).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        return ResponseEntity.ok(mprService.getBPNL_To_MaterialNumberMap(ownMaterialNumber));
    }

    @GetMapping("products")
    @ResponseBody
    @Operation(description = "Returns a list of all products (excluding materials)")
    public List<FrontendMaterialDto> getProducts() {
        return materialService.findAllProducts()
            .stream()
            .map(mat -> new FrontendMaterialDto(mat.getOwnMaterialNumber(), mat.getName()))
            .collect(Collectors.toList());
    }


    @GetMapping("product-stocks")
    @ResponseBody
    @Operation(description = "Returns a list of all product-stocks")
    public List<ProductStockDto> getProductStocks() {
        return productStockService.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }

    @PostMapping("product-stocks")
    @ResponseBody
    @Operation(description = "Creates a new product-stock")
    public ProductStockDto createProductStocks(@RequestBody ProductStockDto productStockDto) {

        ProductStock productStockToCreate = convertToEntity(productStockDto);

        productStockToCreate.setLastUpdatedOn(new Date());

        ProductStock createdProductStock = productStockService.create(productStockToCreate);
        if (createdProductStock == null) {
            return null;
        }
        log.info("Created product-stock: " + createdProductStock);

        return convertToDto(createdProductStock);
    }

    @PutMapping("product-stocks")
    @ResponseBody
    @Operation(description = "Updates an existing product-stock")
    public ProductStockDto updateProductStocks(@RequestBody ProductStockDto productStockDto) {
        ProductStock existingProductStock = productStockService.findByUuid(productStockDto.getUuid());
        if (existingProductStock.getUuid() == null) {
            return null;
        }

        existingProductStock.setQuantity(productStockDto.getQuantity());
        existingProductStock.setMeasurementUnit(productStockDto.getMeasurementUnit());
        existingProductStock.setLastUpdatedOn(new Date());

        existingProductStock = productStockService.update(existingProductStock);
        log.info("Updated product-stock: " + existingProductStock);

        return convertToDto(existingProductStock);
    }

    private ProductStockDto convertToDto(ProductStock entity) {
        ProductStockDto dto = modelMapper.map(entity, ProductStockDto.class);
        dto.getMaterial().setMaterialNumberSupplier(entity.getMaterial().getOwnMaterialNumber());
        var materialPartnerRelation =
            mprService.find(entity.getMaterial().getOwnMaterialNumber(), entity.getAllocatedToCustomerPartner().getUuid());
        dto.getMaterial().setMaterialNumberCustomer(materialPartnerRelation.getPartnerMaterialNumber());

        Partner myself = partnerService.getOwnPartnerEntity();
        setBpnaAndBpnsOnStockDtoBasedOnPartner(entity, dto, myself);

        return dto;
    }

    private ProductStock convertToEntity(ProductStockDto dto) {
        ProductStock productStock = modelMapper.map(dto, ProductStock.class);
        Material material = materialService.findByOwnMaterialNumber(dto.getMaterial().getMaterialNumberSupplier());
        productStock.setMaterial(material);

        PartnerDto allocationPartner = dto.getAllocatedToPartner();

        Partner existingPartner;
        if(allocationPartner.getUuid() != null){

            existingPartner = partnerService.findByUuid(allocationPartner.getUuid());
        }else{
            existingPartner = partnerService.findByBpnl(allocationPartner.getBpnl());
        }

        if (existingPartner == null){
            throw new IllegalStateException(String.format(
                "Partner for uuid %s and bpnl %s could not be found",
                allocationPartner.getUuid(),
                allocationPartner.getBpnl())
            );
        }
        productStock.setAllocatedToCustomerPartner(existingPartner);

        // always set to bpna and find corresponding site
        productStock.setLocationId(dto.getStockLocationBpna());
        productStock.setLocationIdType(LocationIdTypeEnum.B_P_N_A);
        return productStock;
    }

    @GetMapping("material-stocks")
    @ResponseBody
    @Operation(description = "Returns a list of all material-stocks")
    public List<MaterialStockDto> getMaterialStocks() {
        List<MaterialStockDto> allMaterialStocks = materialStockService.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        log.info(allMaterialStocks.toString());
        return allMaterialStocks;
    }

    @PostMapping("material-stocks")
    @ResponseBody
    @Operation(description = "Creates a new material-stock")
    public MaterialStockDto createMaterialStocks(@RequestBody MaterialStockDto materialStockDto) {

        MaterialStock materialStockToCreate = convertToEntity(materialStockDto);
        materialStockToCreate.setLastUpdatedOn(new Date());

        MaterialStock createdMaterialStock = materialStockService.create(materialStockToCreate);

        return convertToDto(createdMaterialStock);
    }

    @PutMapping("material-stocks")
    @ResponseBody
    @Operation(description = "Updates an existing material-stock")
    public MaterialStockDto updateMaterialStocks(@RequestBody MaterialStockDto materialStockDto) {
        MaterialStock existingMaterialStock = materialStockService.findByUuid(materialStockDto.getUuid());
        if (existingMaterialStock == null || existingMaterialStock.getUuid() == null) {
            log.warn("unable to find existing stock, exiting");
            return null;
        }

        existingMaterialStock.setQuantity(materialStockDto.getQuantity());
        existingMaterialStock.setMeasurementUnit(materialStockDto.getMeasurementUnit());
        existingMaterialStock.setLastUpdatedOn(new Date());

        existingMaterialStock = materialStockService.update(existingMaterialStock);

        return convertToDto(existingMaterialStock);
    }

    private MaterialStockDto convertToDto(MaterialStock entity) {
        MaterialStockDto dto = modelMapper.map(entity, MaterialStockDto.class);
        dto.getMaterial().setMaterialNumberCx(entity.getMaterial().getMaterialNumberCx());
        dto.getMaterial().setMaterialNumberCustomer(entity.getMaterial().getOwnMaterialNumber());

        dto.setStockLocationBpns(variablesService.getOwnDefaultBpns());
        dto.setStockLocationBpna(variablesService.getOwnDefaultBpna());
        log.info(dto.toString());

        Partner myself = partnerService.getOwnPartnerEntity();
        setBpnaAndBpnsOnStockDtoBasedOnPartner(entity, dto, myself);

        return dto;
    }

    private MaterialStock convertToEntity(MaterialStockDto dto) {
        MaterialStock materialStock = modelMapper.map(dto, MaterialStock.class);
        materialStock.getMaterial().setOwnMaterialNumber(dto.getMaterial().getMaterialNumberCustomer());

        // always set to bpna and find corresponding site
        materialStock.setLocationId(dto.getStockLocationBpna());
        materialStock.setLocationIdType(LocationIdTypeEnum.B_P_N_A);

        return materialStock;
    }

    @GetMapping("partner-product-stocks")
    @Operation(description = "Returns a list of all partner-product-stocks that refer to the given material number")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid parameter")
    })
    public ResponseEntity<List<PartnerProductStockDto>> getPartnerProductStocks(@RequestParam String ownMaterialNumber) {
        if(!materialPattern.matcher(ownMaterialNumber).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        return ResponseEntity.ok(partnerProductStockService.
            findAllByOwnMaterialNumber(ownMaterialNumber)
            .stream()
            .map(this::convertToDto)
            .collect(Collectors.toList()));
    }

    private PartnerProductStockDto convertToDto(PartnerProductStock entity) {
        PartnerProductStockDto dto = modelMapper.map(entity, PartnerProductStockDto.class);
        dto.getMaterial().setMaterialNumberCx(entity.getMaterial().getMaterialNumberCx());
        dto.getMaterial().setMaterialNumberCustomer(entity.getMaterial().getOwnMaterialNumber());
        var materialPartnerRelation = mprService.find(entity.getMaterial().getOwnMaterialNumber(),
            entity.getSupplierPartner().getUuid());
        dto.getMaterial().setMaterialNumberSupplier(materialPartnerRelation.getPartnerMaterialNumber());

        Partner customerPartner = partnerService.findByBpnl(entity.getSupplierPartner().getBpnl());
        setBpnaAndBpnsOnStockDtoBasedOnPartner(entity, dto, customerPartner);

        return dto;
    }

    /**
     * helper method to lookup and set the corresponding bpna or bpns on dto
     *
     * @param entity Stock to set the data from
     * @param dto StockDto to set the data on
     * @param partner holding the stock
     */
    private void setBpnaAndBpnsOnStockDtoBasedOnPartner(Stock entity, StockDto dto, Partner partner){
        if (entity.getLocationIdType() == LocationIdTypeEnum.B_P_N_A){
            Optional<Site> siteForAddress = partner.getSites()
                .stream().filter(site -> site.getAddresses().stream().anyMatch(addr -> addr.getBpna().equals(entity.getLocationId()))).findFirst();

            if (!siteForAddress.isEmpty()){
                dto.setStockLocationBpns(siteForAddress.get().getBpns());
                dto.setStockLocationBpna(entity.getLocationId());
            }
        }else{
            dto.setStockLocationBpns(entity.getLocationId());
            Optional<Site> siteOfPartner = partner.getSites().stream().filter(site -> site.getBpns().equals(entity.getLocationId())).findFirst();
            if(!siteOfPartner.isEmpty()){
                dto.setStockLocationBpna(siteOfPartner.get().getAddresses().first().getBpna());
            }else{
                throw new IllegalStateException(String.format("Partner %s with Site %s has no Address.", partner.getBpnl(), siteOfPartner.get().getBpns()));
            }
        }
    }

    @GetMapping("customer")
    @Operation(description = "Returns a list of all Partners that are ordering the given material")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid parameter")
    })
    public ResponseEntity<List<PartnerDto>> getCustomerPartnersOrderingMaterial(@RequestParam String ownMaterialNumber) {
        if(!materialPattern.matcher(ownMaterialNumber).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        return ResponseEntity.ok(partnerService.findAllCustomerPartnersForMaterialId(ownMaterialNumber).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList()));
    }

    @GetMapping("supplier")
    @Operation(description = "Returns a list of all Partners that are supplying the given material")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid parameter")
    })
    public ResponseEntity<List<PartnerDto>> getSupplierPartnersSupplyingMaterial(@RequestParam String ownMaterialNumber) {
        if(!materialPattern.matcher(ownMaterialNumber).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        return ResponseEntity.ok(partnerService.findAllSupplierPartnersForMaterialId(ownMaterialNumber).stream()
            .map(this::convertToDto)
            .collect(Collectors.toList()));
    }

    @GetMapping("update-partner-product-stock")
    @Operation(description = "For the given material, all known suppliers will be requested to report their" +
        "current product-stocks. The response body contains a list of those supplier partners that were sent a request." +
        "Please note that these requests are handled asynchronously by the partners, so there are no guarantees, if and " +
        "when the corresponding responses will be available. As soon as a response arrives, it will be available via a " +
        "call to the GET partner-product-stocks endpoint.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Invalid parameter")
    })
    public ResponseEntity<List<PartnerDto>> triggerPartnerProductStockUpdateForMaterial(@RequestParam String ownMaterialNumber) {
        if(!materialPattern.matcher(ownMaterialNumber).matches()) {
            return new ResponseEntity<>(HttpStatusCode.valueOf(400));
        }
        Material materialEntity = materialService.findByOwnMaterialNumber(ownMaterialNumber);
        log.info("Found material: " + (materialEntity != null) + " " + ownMaterialNumber);
        List<Partner> allSupplierPartnerEntities = mprService.findAllSuppliersForOwnMaterialNumber(ownMaterialNumber);

        for (Partner supplierPartner : allSupplierPartnerEntities) {
            productStockRequestApiService.doRequest(materialEntity, supplierPartner);
        }

        return ResponseEntity.ok(allSupplierPartnerEntities.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList()));
    }

    private PartnerDto convertToDto(Partner entity) {
        return modelMapper.map(entity, PartnerDto.class);
    }

}
