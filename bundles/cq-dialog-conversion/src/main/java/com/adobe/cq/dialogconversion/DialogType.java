/*******************************************************************************
 * ADOBE CONFIDENTIAL
 * __________________
 *
 * Copyright 2017 Adobe Systems Incorporated
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 ******************************************************************************/

package com.adobe.cq.dialogconversion;

/**
 * Enumeration that defines the type of a dialog Node in the repository
 */
public enum DialogType {

    /**
     * Classic dialog
     */
    CLASSIC("Classic"),

    /**
     * Granite UI based dialog that uses (legacy) Coral 2 resource types
     */
    CORAL_2("Coral 2"),

    /**
     * Granite UI based dialog that uses Coral 3 resource types
     */
    CORAL_3("Coral 3"),

    /**
     * Dialog type is unknown
     */
    UNKNOWN("");

    private final String text;

    DialogType(String text) {
        this.text = text;
    }

    public String getString() {
        return text;
    }

}
