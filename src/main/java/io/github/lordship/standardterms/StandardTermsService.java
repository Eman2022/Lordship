package io.github.lordship.standardterms;

import io.github.lordship.audit.AuditContext;
import io.github.lordship.audit.AuditMapper;
import io.github.lordship.audit.AuditService;
import io.github.lordship.shared.AgreementType;
import io.github.lordship.standardterms.internal.StandardTermsRepository;
import io.github.lordship.standardterms.internal.StandardTermsRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
public class StandardTermsService {

    private static final Set<String> FEE_METHODS = Set.of("NONE", "FLAT");
    private static final Set<String> UTILITY_METHODS = Set.of("NONE", "FLAT", "RUBS", "SUBMETERED");
    private static final Set<String> TRASH_METHODS = Set.of("NONE", "FLAT", "RUBS");

    // if only these things change - DO NOT do an audit log
    private static final Set<String> HOUSEKEEPING_KEYS = Set.of("updatedAt", "updatedBy");

    private record MethodAmountPair(
            String methodColumn,
            String amountColumn,
            Set<String> allowedMethods,
            Function<StandardTermsRow, Enum<?>> currentMethod,
            Function<StandardTermsRow, BigDecimal> currentAmount) {}

    private static final List<MethodAmountPair> METHOD_AMOUNT_PAIRS = List.of(
            new MethodAmountPair("late_fee_method", "late_fee_amount", FEE_METHODS,
                    StandardTermsRow::lateFeeMethod, StandardTermsRow::lateFeeAmount),
            new MethodAmountPair("rule_violation_fee_method", "rule_violation_fee_amount", FEE_METHODS,
                    StandardTermsRow::ruleViolationFeeMethod, StandardTermsRow::ruleViolationFeeAmount),
            new MethodAmountPair("nsf_fee_method", "nsf_fee_amount", FEE_METHODS,
                    StandardTermsRow::nsfFeeMethod, StandardTermsRow::nsfFeeAmount),
            new MethodAmountPair("water_method", "water_flat_amount", UTILITY_METHODS,
                    StandardTermsRow::waterMethod, StandardTermsRow::waterFlatAmount),
            new MethodAmountPair("power_method", "power_flat_amount", UTILITY_METHODS,
                    StandardTermsRow::powerMethod, StandardTermsRow::powerFlatAmount),
            new MethodAmountPair("sewer_method", "sewer_flat_amount", UTILITY_METHODS,
                    StandardTermsRow::sewerMethod, StandardTermsRow::sewerFlatAmount),
            new MethodAmountPair("trash_method", "trash_flat_amount", TRASH_METHODS,
                    StandardTermsRow::trashMethod, StandardTermsRow::trashFlatAmount));

    private final StandardTermsRepository standardTermsRepository;
    private final AuditService auditService;
    private final AuditContext auditContext;

    public StandardTermsService(StandardTermsRepository standardTermsRepository,
                                AuditService auditService,
                                AuditContext auditContext) {
        this.standardTermsRepository = standardTermsRepository;
        this.auditService = auditService;
        this.auditContext = auditContext;
    }

    public Optional<StandardTerms> findById(UUID uuid) {
        return standardTermsRepository.findById(uuid).map(StandardTermsRow::toStandardTerms);
    }

    // The deal types this property may offer.
    public List<StandardTerms> findByProperty(UUID property) {
        return standardTermsRepository.findByProperty(property).stream()
                .map(StandardTermsRow::toStandardTerms)
                .toList();
    }

    // The admin-only pool a property copies from.
    public List<StandardTerms> findGlobalTemplates() {
        return standardTermsRepository.findGlobalTemplates().stream()
                .map(StandardTermsRow::toStandardTerms)
                .toList();
    }

    // Used when a tenancy is created: the terms it starts from.
    public Optional<StandardTerms> findForProperty(UUID property, AgreementType agreementType) {
        return standardTermsRepository.findByPropertyAndAgreementType(property, agreementType)
                .map(StandardTermsRow::toStandardTerms);
    }

    @Transactional
    public StandardTerms createGlobalTemplate(String name, AgreementType agreementType) {
        requireScopeIsFree(null, agreementType);

        StandardTermsRow saved = standardTermsRepository.save(
                new StandardTermsRow(null, name, agreementType));

        auditService.recordInsert("standard_terms", saved.uuid(), AuditMapper.toMap(saved));
        return saved.toStandardTerms();
    }

    // A property gains an agreement type only by an admin copying a global template in.
    // Empty means the template does not exist.
    @Transactional
    public Optional<StandardTerms> copyTemplateToProperty(UUID templateUuid, UUID property) {
        Optional<StandardTermsRow> templateOpt = standardTermsRepository.findById(templateUuid);
        if (templateOpt.isEmpty()) {
            return Optional.empty();
        }
        StandardTermsRow template = templateOpt.get();

        if (template.property() != null) {
            throw new IllegalArgumentException("Only a global template can be copied into a property");
        }
        requireScopeIsFree(property, template.agreementType());

        StandardTermsRow saved = standardTermsRepository.saveCopy(template.copyTo(property));
        auditService.recordInsert("standard_terms", saved.uuid(), AuditMapper.toMap(saved));
        return Optional.of(saved.toStandardTerms());
    }

    @Transactional
    public Optional<StandardTerms> patchStandardTerms(UUID uuid, Map<String, Object> changes) {
        Optional<StandardTermsRow> beforeOpt = standardTermsRepository.findById(uuid);
        if (beforeOpt.isEmpty()) {
            return Optional.empty();
        }
        StandardTermsRow before = beforeOpt.get();

        reconcileMethodAmountPairs(before, changes);

        Optional<StandardTermsRow> afterOpt =
                standardTermsRepository.patch(uuid, changes, auditContext.getActingUserId());
        if (afterOpt.isEmpty()) {
            return Optional.empty();
        }
        StandardTermsRow after = afterOpt.get();

        AuditMapper.Diff diff = AuditMapper.diff(before, after);
        Map<String, Object> changedBefore = withoutHousekeeping(diff.before());
        Map<String, Object> changedAfter = withoutHousekeeping(diff.after());
        if (!changedBefore.isEmpty()) {
            auditService.recordUpdate("standard_terms", uuid, changedBefore, changedAfter);
        }

        return Optional.of(after.toStandardTerms());
    }

    @Transactional
    public boolean deleteStandardTerms(UUID uuid) {
        return standardTermsRepository.findById(uuid).map(existing -> {
            if (!standardTermsRepository.softDelete(uuid)) {
                return false;
            }
            auditService.recordDelete("standard_terms", uuid, AuditMapper.toMap(existing));
            return true;
        }).orElse(false);
    }

    // At most one set per scope per agreement type -- global (property null) included.
    private void requireScopeIsFree(UUID property, AgreementType agreementType) {
        if (standardTermsRepository.findByPropertyAndAgreementType(property, agreementType).isPresent()) {
            throw new IllegalStateException(
                    "A standard terms set already exists for agreement type " + agreementType);
        }
    }

    // A flat amount is only meaningful when the method is FLAT; every other method
    // requires it to be zero. Resolve the resulting pair from `before` plus the patch,
    // so patching either half alone still lands on a row the CHECK constraints accept.
    private static void reconcileMethodAmountPairs(StandardTermsRow before, Map<String, Object> changes) {
        for (MethodAmountPair pair : METHOD_AMOUNT_PAIRS) {
            boolean methodTouched = changes.containsKey(pair.methodColumn());
            boolean amountTouched = changes.containsKey(pair.amountColumn());
            if (!methodTouched && !amountTouched) {
                continue;
            }

            String method = nameOf(pair.currentMethod().apply(before));
            if (methodTouched) {
                Object raw = changes.get(pair.methodColumn());
                method = (raw == null) ? null : raw.toString().trim().toUpperCase(Locale.ROOT);
                if (!pair.allowedMethods().contains(method)) {
                    throw new IllegalArgumentException(
                            pair.methodColumn() + " must be one of " + pair.allowedMethods());
                }
                changes.put(pair.methodColumn(), method);
            }

            if (!"FLAT".equals(method)) {
                changes.put(pair.amountColumn(), BigDecimal.ZERO);
                continue;
            }

            BigDecimal amount = amountTouched
                    ? toAmount(changes.get(pair.amountColumn()), pair.amountColumn())
                    : pair.currentAmount().apply(before);

            if (amount == null || amount.signum() <= 0) {
                throw new IllegalArgumentException(
                        pair.amountColumn() + " must be greater than zero when "
                                + pair.methodColumn() + " is FLAT");
            }
            changes.put(pair.amountColumn(), amount);
        }
    }

    private static Map<String, Object> withoutHousekeeping(Map<String, Object> map) {
        Map<String, Object> copy = new LinkedHashMap<>(map);
        copy.keySet().removeAll(HOUSEKEEPING_KEYS);
        return copy;
    }

    private static BigDecimal toAmount(Object raw, String column) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof BigDecimal decimal) {
            return decimal;
        }
        try {
            String text = raw.toString().trim();
            return text.isEmpty() ? null : new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(column + " must be a number");
        }
    }

    private static String nameOf(Enum<?> value) {
        return value == null ? null : value.name();
    }
}