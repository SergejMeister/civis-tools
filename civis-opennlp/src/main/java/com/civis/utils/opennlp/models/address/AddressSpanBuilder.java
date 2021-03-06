/*
 * Copyright 2015 Sergej Meister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.civis.utils.opennlp.models.address;

import com.civis.utils.csv.address.CSVAddressData;
import com.civis.utils.opennlp.features.StreetNumberFeature;
import opennlp.tools.util.Span;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class to build a address span.
 */
public class AddressSpanBuilder {

    private final double probability;
    private final Span originalSpan;
    private final String[] tokens;
    private String street;
    private String streetNumber;
    private String city;
    private String zip;
    private String country;
    private Set<String> countries;
    private List<CSVAddressData> csvAddressDataList;

    public AddressSpanBuilder(Span span, double probability, String[] tokens) {
        this.originalSpan = span;
        this.probability = probability;
        this.tokens = tokens;
        this.countries = new HashSet<>();
        this.csvAddressDataList = new ArrayList<>();
    }

    public AddressSpanBuilder setCountries(Set<String> countries) {
        this.countries.addAll(countries);
        return this;
    }

    public AddressSpanBuilder setCsvAddressData(List<CSVAddressData> csvAddressDataList) {
        this.csvAddressDataList.addAll(csvAddressDataList);
        return this;
    }

    public AddressSpan build() {
        parse(tokens);
        AddressSpan addressSpan = new AddressSpan();
        addressSpan.setProbability(probability);
        addressSpan.setStreet(street);
        addressSpan.setStreetNumber(streetNumber);
        addressSpan.setZip(zip);
        addressSpan.setCity(city);
        addressSpan.setCountry(country);
        return addressSpan;
    }

    private void parse(String[] tokens) {
        Span streetSpan = createStreetSpan(originalSpan.getStart(), originalSpan.getEnd(), tokens);
        street = buildString(streetSpan, tokens);
        Span streetNumberSpan = new Span(streetSpan.getEnd(), streetSpan.getEnd() + 1);
        streetNumber = buildString(streetNumberSpan, tokens);
        Span zipSpan = new Span(streetNumberSpan.getEnd(), streetNumberSpan.getEnd() + 1);
        zip = buildString(zipSpan, tokens);
        zip = zip.replaceAll("[+.^:,]", "");
        if (StringUtils.isBlank(zip)) {
            // token include only special chars like , or .
            //try next zip token
            // use case Lindenstr. 19 , 12207 Berlin
            zipSpan = new Span(zipSpan.getStart() + 1, zipSpan.getEnd() + 1);
            zip = buildString(zipSpan, tokens);
        }

        CSVAddressData csvAddressData = findAddressDataByZip(zip);
        if (csvAddressData != null) {
            city = csvAddressData.getCity();
            country = "Deutschland";
        } else {
            String cityAndMaybeCountry = buildString(zipSpan.getEnd(), originalSpan.getEnd(), tokens);
            country = tryToFindCountry(cityAndMaybeCountry);
            if (country == null) {
                // no country found, means rest string is a city string
                city = cityAndMaybeCountry;
            } else {
                city = cityAndMaybeCountry.replace(country, "").trim();
            }
        }
    }

    private String tryToFindCountry(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        for (String country : countries) {
            if (value.endsWith(country)) {
                return country;
            }
        }
        return null;
    }

    private CSVAddressData findAddressDataByZip(String zip) {
        for (CSVAddressData csvAddressData : csvAddressDataList) {
            if (csvAddressData.getZip().equals(zip)) {
                return csvAddressData;
            }
        }
        return null;
    }

    private String buildString(Span span, String[] tokens) {
        return buildString(span.getStart(), span.getEnd(), tokens);
    }

    private String buildString(int start, int end, String[] tokens) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.append(tokens[i]);
            sb.append(" ");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private Span createStreetSpan(int start, int end, String[] tokens) {
        for (int i = start; i < end; i++) {
            if (StreetNumberFeature.STREET_NUMBER_PATTERN.matcher(tokens[i]).matches()) {
                return new Span(start, i);
            }
        }

        return new Span(start, end);
    }

}
