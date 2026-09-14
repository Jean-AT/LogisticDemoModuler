package com.logistica.demo.platform.controller;

import com.logistica.demo.platform.api.CatalogItemReference;
import com.logistica.demo.platform.api.CurrencyReference;
import com.logistica.demo.platform.api.DefineFiscalPeriodCommand;
import com.logistica.demo.platform.api.DocumentNumber;
import com.logistica.demo.platform.api.DocumentSequencePort;
import com.logistica.demo.platform.api.FiscalPeriod;
import com.logistica.demo.platform.api.FiscalPeriodAdministration;
import com.logistica.demo.platform.api.FiscalPeriodQuery;
import com.logistica.demo.platform.api.MasterDataReference;
import com.logistica.demo.platform.api.PlatformCatalogQuery;
import com.logistica.demo.platform.api.UnitOfMeasureReference;
import com.logistica.demo.platform.api.UserAccessProfile;
import com.logistica.demo.platform.api.UserAccessQuery;
import com.logistica.demo.platform.dto.ConfigureDocumentSequenceRequest;
import com.logistica.demo.platform.dto.DefineFiscalPeriodRequest;
import com.logistica.demo.platform.dto.NextDocumentNumberRequest;
import com.logistica.demo.shared.exception.BadRequestException;
import com.logistica.demo.shared.exception.BusinessRuleException;
import com.logistica.demo.shared.exception.ResourceNotFoundException;
import com.logistica.demo.shared.security.CurrentUserService;
import com.logistica.demo.sharedkernel.web.ApiPaths;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/platform")
public class PlatformController {

    private final PlatformCatalogQuery catalogQuery;
    private final UserAccessQuery userAccessQuery;
    private final FiscalPeriodQuery fiscalPeriodQuery;
    private final FiscalPeriodAdministration fiscalPeriodAdministration;
    private final DocumentSequencePort documentSequencePort;
    private final CurrentUserService currentUserService;

    public PlatformController(
            PlatformCatalogQuery catalogQuery,
            UserAccessQuery userAccessQuery,
            FiscalPeriodQuery fiscalPeriodQuery,
            FiscalPeriodAdministration fiscalPeriodAdministration,
            DocumentSequencePort documentSequencePort,
            CurrentUserService currentUserService) {
        this.catalogQuery = catalogQuery;
        this.userAccessQuery = userAccessQuery;
        this.fiscalPeriodQuery = fiscalPeriodQuery;
        this.fiscalPeriodAdministration = fiscalPeriodAdministration;
        this.documentSequencePort = documentSequencePort;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/catalog/companies")
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.READ')")
    public List<MasterDataReference> companies() {
        return catalogQuery.findActiveCompanies();
    }

    @GetMapping("/catalog/companies/{companyId}")
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.READ')")
    public MasterDataReference company(@PathVariable Long companyId) {
        return catalogQuery.findActiveCompany(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Compania no encontrada"));
    }

    @GetMapping("/catalog/cost-centers")
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.READ')")
    public List<MasterDataReference> costCenters(@RequestParam Long companyId) {
        return catalogQuery.findActiveCostCenters(companyId);
    }

    @GetMapping("/catalog/financing-sources")
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.READ')")
    public List<MasterDataReference> financingSources(@RequestParam Long companyId) {
        return catalogQuery.findActiveFinancingSources(companyId);
    }

    @GetMapping("/catalog/goals")
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.READ')")
    public List<MasterDataReference> goals(@RequestParam Long companyId, @RequestParam int fiscalYear) {
        return catalogQuery.findActiveGoals(companyId, fiscalYear);
    }

    @GetMapping("/catalog/expense-classifiers")
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.READ')")
    public List<MasterDataReference> expenseClassifiers(@RequestParam Long companyId) {
        return catalogQuery.findActiveExpenseClassifiers(companyId);
    }

    @GetMapping("/catalog/items")
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.READ')")
    public List<CatalogItemReference> items(@RequestParam Long companyId) {
        return catalogQuery.findActiveCatalogItems(companyId);
    }

    @GetMapping("/catalog/items/{itemCode}")
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.READ')")
    public CatalogItemReference item(@RequestParam Long companyId, @PathVariable String itemCode) {
        return catalogQuery.findActiveCatalogItemByCode(companyId, itemCode)
                .orElseThrow(() -> new ResourceNotFoundException("Bien o servicio no encontrado"));
    }

    @GetMapping("/catalog/currencies/{currencyCode}")
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.READ')")
    public CurrencyReference currency(@PathVariable String currencyCode) {
        return catalogQuery.findActiveCurrency(currencyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Moneda no encontrada"));
    }

    @GetMapping("/catalog/units/{unitCode}")
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.READ')")
    public UnitOfMeasureReference unit(@PathVariable String unitCode) {
        return catalogQuery.findActiveUnitOfMeasure(unitCode)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de medida no encontrada"));
    }

    @GetMapping("/security/users/{username}/access")
    @PreAuthorize("hasAuthority('PLATFORM.SECURITY.WRITE')")
    public UserAccessProfile userAccess(@PathVariable String username) {
        return userAccessQuery.findActiveByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de acceso no encontrado"));
    }

    @GetMapping("/fiscal-periods")
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.READ')")
    public FiscalPeriod fiscalPeriod(
            @RequestParam Long companyId,
            @RequestParam(required = false) Integer fiscalYear,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date != null) {
            return fiscalPeriodQuery.findByCompanyAndDate(companyId, date)
                    .orElseThrow(() -> new ResourceNotFoundException("Periodo fiscal no encontrado"));
        }
        if (fiscalYear == null || month == null) {
            throw new BadRequestException("Debe enviar date o fiscalYear y month");
        }
        return fiscalPeriodQuery.findByCompanyYearAndMonth(companyId, fiscalYear, month)
                .orElseThrow(() -> new ResourceNotFoundException("Periodo fiscal no encontrado"));
    }

    @PutMapping("/fiscal-periods")
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.WRITE')")
    public FiscalPeriod defineFiscalPeriod(@RequestBody DefineFiscalPeriodRequest request) {
        try {
            return fiscalPeriodAdministration.definePeriod(new DefineFiscalPeriodCommand(
                    request.companyId(),
                    request.fiscalYear(),
                    request.month(),
                    request.startsOn(),
                    request.endsOn(),
                    request.status(),
                    currentUserService.getUsername()));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    @PostMapping("/fiscal-periods/{companyId}/{fiscalYear}/{month}/open")
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.WRITE')")
    public FiscalPeriod openFiscalPeriod(
            @PathVariable Long companyId,
            @PathVariable int fiscalYear,
            @PathVariable int month) {
        return changePeriodStatus(companyId, fiscalYear, month, true);
    }

    @PostMapping("/fiscal-periods/{companyId}/{fiscalYear}/{month}/close")
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.WRITE')")
    public FiscalPeriod closeFiscalPeriod(
            @PathVariable Long companyId,
            @PathVariable int fiscalYear,
            @PathVariable int month) {
        return changePeriodStatus(companyId, fiscalYear, month, false);
    }

    @PutMapping("/document-sequences")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.WRITE')")
    public void configureDocumentSequence(@RequestBody ConfigureDocumentSequenceRequest request) {
        try {
            documentSequencePort.configureSequence(
                    request.companyId(),
                    request.fiscalYear(),
                    request.documentType(),
                    request.prefix(),
                    request.currentValue());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    @PostMapping("/document-sequences/next")
    @PreAuthorize("hasAuthority('PLATFORM.MASTER.WRITE')")
    public DocumentNumber nextDocumentNumber(@RequestBody NextDocumentNumberRequest request) {
        try {
            return documentSequencePort.nextDocumentNumber(
                    request.companyId(),
                    request.fiscalYear(),
                    request.documentType());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    private FiscalPeriod changePeriodStatus(Long companyId, int fiscalYear, int month, boolean open) {
        try {
            if (open) {
                return fiscalPeriodAdministration.openPeriod(companyId, fiscalYear, month, currentUserService.getUsername());
            }
            return fiscalPeriodAdministration.closePeriod(companyId, fiscalYear, month, currentUserService.getUsername());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new BusinessRuleException(ex.getMessage());
        }
    }
}
