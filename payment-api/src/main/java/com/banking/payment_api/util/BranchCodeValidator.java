package com.banking.payment_api.util;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
//this will validate South African bank branch code
public class BranchCodeValidator {

    //list of valid know SA bank branch code
    private static final Set<String> VALID_BRANCH_CODES = Set.of(
            "250655", //FNB
            "470010", //Capitec Bank
            "632005", //ABSA
            "051001" //Standard Bank
    );

    public boolean isValid(String branchCode) {
        //this will check if the branch is a known South African bank branch code

        if (branchCode == null || branchCode.isBlank()) {
            return true;
            //meaning branch code is optional still if it was not provided still valid
            //only validate it when is provided
        }

        return VALID_BRANCH_CODES.contains(branchCode);
        //check if provided code is in the list of known valid codes(the ones i mentioned above)
    }
}
