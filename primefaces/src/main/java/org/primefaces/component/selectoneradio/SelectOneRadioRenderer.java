/*
 * The MIT License
 *
 * Copyright (c) 2009-2023 PrimeTek Informatics
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.primefaces.component.selectoneradio;

import java.io.IOException;
import java.util.List;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UISelectOne;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.model.SelectItem;
import javax.faces.render.Renderer;

import org.primefaces.renderkit.SelectOneRenderer;
import org.primefaces.util.*;

public class SelectOneRadioRenderer extends SelectOneRenderer {

    @Override
    public Object getConvertedValue(FacesContext context, UIComponent component, Object submittedValue) throws ConverterException {
        Renderer renderer = ComponentUtils.getUnwrappedRenderer(
                context,
                "javax.faces.SelectOne",
                "javax.faces.Radio");
        return renderer.getConvertedValue(context, component, submittedValue);
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        SelectOneRadio radio = (SelectOneRadio) component;

        encodeMarkup(context, radio);
        encodeScript(context, radio);
    }

    protected void encodeMarkup(FacesContext context, SelectOneRadio radio) throws IOException {
        String layout = radio.getLayout();
        if (LangUtils.isEmpty(layout)) {
            layout = ComponentUtils.shouldRenderFacet(radio.getFacet("custom")) ? "custom" : "lineDirection";
        }
        boolean custom = "custom".equals(layout);

        if (custom) {
            encodeCustomLayout(context, radio);
        }
        else if ("grid".equals(layout)) {
            encodeLegacyTabularLayout(context, radio, layout);
        }
        else {
            encodeResponsiveLayout(context, radio, layout);
        }
    }

    protected void encodeScript(FacesContext context, SelectOneRadio radio) throws IOException {
        String layout = radio.getLayout();
        if (LangUtils.isEmpty(layout) && ComponentUtils.shouldRenderFacet(radio.getFacet("custom"))) {
            layout = "custom";
        }
        boolean custom = "custom".equals(layout);

        WidgetBuilder wb = getWidgetBuilder(context);
        wb.init("SelectOneRadio", radio)
                .attr("custom", custom, false)
                .attr("unselectable", radio.isUnselectable())
                .attr("readonly", radio.isReadonly(), false)
                .finish();
    }

    protected void encodeResponsiveLayout(FacesContext context, SelectOneRadio radio, String layout) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String clientId = radio.getClientId(context);
        List<SelectItem> selectItems = getSelectItems(context, radio);
        String style = radio.getStyle();
        boolean flex = ComponentUtils.isFlex(context, radio);
        if (flex) {
            layout = "responsive";
        }
        boolean lineDirection = "lineDirection".equals(layout);
        String styleClass = getStyleClassBuilder(context)
                .add(lineDirection, "layout-line-direction")
                .add(GridLayoutUtils.getResponsiveClass(flex))
                .add(radio.getStyleClass())
                .add(SelectOneRadio.STYLE_CLASS)
                .build();
        String labelledBy = radio.getLabel();

        writer.startElement("div", radio);
        writer.writeAttribute("id", clientId, "id");
        writer.writeAttribute("role", "radiogroup", null);
        if (labelledBy != null) {
            writer.writeAttribute("aria-labelledby", labelledBy, "label");
        }
        writer.writeAttribute("class", styleClass, "styleClass");
        if (style != null) {
            writer.writeAttribute("style", style, "style");
        }
        renderARIARequired(context, radio);

        Converter converter = radio.getConverter();
        String name = radio.getClientId(context);
        String currentValue = ComponentUtils.getValueToRender(context, radio);

        int columns = radio.getColumns();
        if (lineDirection || "pageDirection".equals(layout)) {
            columns = 1;
        }

        if (columns > 0) {
            int idx = 0;
            int colMod;

            for (int i = 0; i < selectItems.size(); i++) {
                SelectItem selectItem = selectItems.get(i);
                boolean disabled = selectItem.isDisabled() || radio.isDisabled();
                String id = name + UINamingContainer.getSeparatorChar(context) + idx;
                boolean selected = isSelected(context, radio, selectItem, currentValue);
                colMod = idx % columns;
                if (!lineDirection && colMod == 0) {
                    writer.startElement("div", null);
                    writer.writeAttribute("class", GridLayoutUtils.getFlexGridClass(flex), null);
                }

                writer.startElement("div", null);
                if (!lineDirection) {
                    writer.writeAttribute("class", GridLayoutUtils.getColumnClass(flex, columns), null);
                }
                writer.writeAttribute("role", "radio", null);
                writer.writeAttribute(HTML.ARIA_CHECKED, Boolean.toString(selected), null);
                encodeOption(context, radio, selectItem, id, name, converter, selected, disabled);
                writer.endElement("div");

                idx++;
                colMod = idx % columns;

                if (!lineDirection && colMod == 0) {
                    writer.endElement("div");
                }
            }

            if (idx != 0 && (idx % columns) != 0) {
                writer.endElement("div");
            }
        }
        else {
            throw new FacesException("The value of columns attribute must be greater than zero.");
        }

        writer.endElement("div");
    }

    /**
     * @deprecated in 13.0.0 remove in 14.0.0
     */
    @Deprecated
    protected void encodeLegacyTabularLayout(FacesContext context, SelectOneRadio radio, String layout) throws IOException {
        String clientId = radio.getClientId(context);
        logDevelopmentWarning(context, "Table layout is deprecated and will be removed in future release. Please switch to responsive layout. ClientId: "
                + clientId);
        ResponseWriter writer = context.getResponseWriter();
        List<SelectItem> selectItems = getSelectItems(context, radio);
        String style = radio.getStyle();
        String styleClass = getStyleClassBuilder(context)
                .add(radio.getStyleClass())
                .add(SelectOneRadio.STYLE_CLASS)
                .build();
        String labelledBy = radio.getLabel();

        writer.startElement("table", radio);
        writer.writeAttribute("id", clientId, "id");
        writer.writeAttribute("role", "radiogroup", null);
        if (labelledBy != null) {
            writer.writeAttribute("aria-labelledby", labelledBy, "label");
        }
        writer.writeAttribute("class", styleClass, "styleClass");
        if (style != null) {
            writer.writeAttribute("style", style, "style");
        }

        renderARIARequired(context, radio);
        encodeSelectItems(context, radio, selectItems, layout);

        writer.endElement("table");
    }

    /**
     * @deprecated in 13.0.0 remove in 14.0.0
     */
    @Deprecated
    protected void encodeSelectItems(FacesContext context, SelectOneRadio radio, List<SelectItem> selectItems, String layout)
            throws IOException {

        if ("lineDirection".equals(layout)) {
            encodeLineLayout(context, radio, selectItems);
        }
        else if ("pageDirection".equals(layout)) {
            encodePageLayout(context, radio, selectItems);
        }
        else if ("grid".equals(layout)) {
            encodeGridLayout(context, radio, selectItems);
        }
        else {
            throw new FacesException("Invalid '" + layout + "' type for component '" + radio.getClientId(context) + "'.");
        }
    }

    protected void encodeCustomLayout(FacesContext context, SelectOneRadio radio) throws IOException {
        UIComponent customFacet = radio.getFacet("custom");
        if (ComponentUtils.shouldRenderFacet(customFacet)) {
            ResponseWriter writer = context.getResponseWriter();
            String style = radio.getStyle();
            String styleClass = getStyleClassBuilder(context)
                    .add(radio.getStyleClass())
                    .add(SelectOneRadio.STYLE_CLASS)
                    .build();
            String labelledBy = radio.getLabel();
            writer.startElement("span", radio);
            writer.writeAttribute("id", radio.getClientId(context), "id");
            writer.writeAttribute("role", "radiogroup", null);
            if (labelledBy != null) {
                writer.writeAttribute("aria-labelledby", labelledBy, "label");
            }
            if (style != null) {
                writer.writeAttribute("style", style, "style");
            }
            if (styleClass != null) {
                writer.writeAttribute("class", styleClass, "styleClass");
            }

            encodeCustomLayoutHelper(context, radio, false);
            customFacet.encodeAll(context);

            writer.endElement("span");
        }
        else {
            encodeCustomLayoutHelper(context, radio, true);
        }
    }

    protected void encodeCustomLayoutHelper(FacesContext context, SelectOneRadio radio, boolean addId) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("span", radio);
        if (addId) {
            writer.writeAttribute("id", radio.getClientId(context), "id");
        }
        writer.writeAttribute("class", "ui-helper-hidden", null);

        Converter converter = radio.getConverter();
        String name = radio.getClientId(context);
        List<SelectItem> selectItems = getSelectItems(context, radio);
        String currentValue = ComponentUtils.getValueToRender(context, radio);

        for (int i = 0; i < selectItems.size(); i++) {
            SelectItem selectItem = selectItems.get(i);
            String id = name + UINamingContainer.getSeparatorChar(context) + i;
            boolean selected = isSelected(context, radio, selectItem, currentValue);
            boolean disabled = selectItem.isDisabled() || radio.isDisabled();
            String itemValueAsString = getOptionAsString(context, radio, converter, selectItem.getValue());
            encodeOptionInput(context, radio, id, name, selected, disabled, itemValueAsString);
        }

        writer.endElement("span");
    }

    /**
     * @deprecated in 13.0.0 remove in 14.0.0
     */
    @Deprecated
    protected void encodeLineLayout(FacesContext context, SelectOneRadio radio, List<SelectItem> selectItems) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        Converter converter = radio.getConverter();
        String name = radio.getClientId(context);
        String currentValue = ComponentUtils.getValueToRender(context, radio);

        writer.startElement("tr", null);
        for (int i = 0; i < selectItems.size(); i++) {
            SelectItem selectItem = selectItems.get(i);
            boolean disabled = selectItem.isDisabled() || radio.isDisabled();
            String id = name + UINamingContainer.getSeparatorChar(context) + i;
            boolean selected = isSelected(context, radio, selectItem, currentValue);

            writer.startElement("td", null);
            writer.writeAttribute("role", "radio", null);
            writer.writeAttribute(HTML.ARIA_CHECKED, Boolean.toString(selected), null);
            encodeOption(context, radio, selectItem, id, name, converter, selected, disabled);
            writer.endElement("td");
        }
        writer.endElement("tr");
    }

    /**
     * @deprecated in 13.0.0 remove in 14.0.0
     */
    @Deprecated
    protected void encodePageLayout(FacesContext context, SelectOneRadio radio, List<SelectItem> selectItems) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        Converter converter = radio.getConverter();
        String name = radio.getClientId(context);
        String currentValue = ComponentUtils.getValueToRender(context, radio);

        for (int i = 0; i < selectItems.size(); i++) {
            SelectItem selectItem = selectItems.get(i);
            boolean disabled = selectItem.isDisabled() || radio.isDisabled();
            String id = name + UINamingContainer.getSeparatorChar(context) + i;
            boolean selected = isSelected(context, radio, selectItem, currentValue);

            writer.startElement("tr", null);
            writer.writeAttribute("role", "radio", null);
            writer.writeAttribute(HTML.ARIA_CHECKED, Boolean.toString(selected), null);
            writer.startElement("td", null);
            encodeOption(context, radio, selectItem, id, name, converter, selected, disabled);
            writer.endElement("td");
            writer.endElement("tr");
        }
    }

    /**
     * @deprecated in 13.0.0 remove in 14.0.0
     */
    @Deprecated
    protected void encodeGridLayout(FacesContext context, SelectOneRadio radio, List<SelectItem> selectItems) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        Converter converter = radio.getConverter();
        String name = radio.getClientId(context);
        int columns = radio.getColumns();
        String currentValue = ComponentUtils.getValueToRender(context, radio);

        if (columns > 0) {
            int idx = 0;
            int colMod;
            int totalItems = selectItems.size();

            for (int i = 0; i < totalItems; i++) {
                SelectItem selectItem = selectItems.get(i);
                boolean disabled = selectItem.isDisabled() || radio.isDisabled();
                String id = name + UINamingContainer.getSeparatorChar(context) + idx;
                boolean selected = isSelected(context, radio, selectItem, currentValue);

                colMod = idx % columns;
                if (colMod == 0) {
                    writer.startElement("tr", null);
                }

                writer.startElement("td", null);
                writer.writeAttribute("role", "radio", null);
                writer.writeAttribute(HTML.ARIA_CHECKED, Boolean.toString(selected), null);
                encodeOption(context, radio, selectItem, id, name, converter, selected, disabled);
                writer.endElement("td");
                idx++;
                colMod = idx % columns;

                if (colMod == 0 || idx == totalItems) {
                    writer.endElement("tr");
                }
            }
        }
        else {
            throw new FacesException("The value of columns attribute must be greater than zero.");
        }
    }

    protected void encodeOption(FacesContext context, SelectOneRadio radio, SelectItem option, String id, String name,
                                Converter converter, boolean selected, boolean disabled) throws IOException {

        ResponseWriter writer = context.getResponseWriter();
        String itemValueAsString = getOptionAsString(context, radio, converter, option.getValue());
        String styleClass = HTML.RADIOBUTTON_CLASS;

        writer.startElement("div", null);
        writer.writeAttribute("class", styleClass, null);

        encodeOptionInput(context, radio, id, name, selected, disabled, itemValueAsString);
        encodeOptionOutput(context, radio, selected, disabled);

        writer.endElement("div");

        encodeOptionLabel(context, radio, id, option, disabled);
    }

    protected void encodeOptionInput(FacesContext context, SelectOneRadio radio, String id, String name, boolean checked,
                                     boolean disabled, String value) throws IOException {

        ResponseWriter writer = context.getResponseWriter();

        writer.startElement("div", null);
        writer.writeAttribute("class", "ui-helper-hidden-accessible", null);

        writer.startElement("input", null);
        writer.writeAttribute("id", id, null);
        writer.writeAttribute("name", name, null);
        writer.writeAttribute("type", "radio", null);
        writer.writeAttribute("value", value, null);

        renderDomEvents(context, radio, SelectOneRadio.DOM_EVENTS);

        if (radio.getTabindex() != null) {
            writer.writeAttribute("tabindex", radio.getTabindex(), null);
        }
        if (checked) {
            writer.writeAttribute("checked", "checked", null);
        }
        if (disabled) {
            writer.writeAttribute("disabled", "disabled", null);
        }

        renderValidationMetadata(context, radio);

        writer.endElement("input");

        writer.endElement("div");
    }

    protected void encodeOptionLabel(FacesContext context, SelectOneRadio radio, String containerClientId, SelectItem option,
                                     boolean disabled) throws IOException {

        ResponseWriter writer = context.getResponseWriter();
        String label = option.getLabel();

        writer.startElement("label", null);
        writer.writeAttribute("for", containerClientId, null);
        if (disabled) {
            writer.writeAttribute("class", "ui-state-disabled", null);
        }

        if (option.getDescription() != null) {
            writer.writeAttribute("title", option.getDescription(), null);
        }

        if (label != null) {
            if (option.isEscape()) {
                writer.writeText(label, null);
            }
            else {
                writer.write(label);
            }
        }

        writer.endElement("label");
    }

    protected void encodeOptionOutput(FacesContext context, SelectOneRadio radio, boolean selected, boolean disabled) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String boxClass = createStyleClass(radio, null, HTML.RADIOBUTTON_BOX_CLASS);
        boxClass = selected ? boxClass + " ui-state-active" : boxClass;
        boxClass = disabled ? boxClass + " ui-state-disabled" : boxClass;
        String iconClass = selected ? HTML.RADIOBUTTON_CHECKED_ICON_CLASS : HTML.RADIOBUTTON_UNCHECKED_ICON_CLASS;

        writer.startElement("div", null);
        writer.writeAttribute("class", boxClass, null);

        writer.startElement("span", null);
        writer.writeAttribute("class", iconClass, null);
        writer.endElement("span");

        writer.endElement("div");
    }

    protected boolean isSelected(FacesContext context, SelectOneRadio radio, SelectItem selectItem, String currentValue) {
        String itemStrValue = getOptionAsString(context, radio, radio.getConverter(), selectItem.getValue());
        return LangUtils.isBlank(itemStrValue)
                ? LangUtils.isBlank(currentValue)
                : itemStrValue.equals(currentValue);
    }

    @Override
    protected String getSubmitParam(FacesContext context, UISelectOne selectOne) {
        return selectOne.getClientId(context);
    }

    @Override
    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        //Do nothing
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    @Override
    public String getHighlighter() {
        return "oneradio";
    }

    @Override
    protected boolean isGrouped() {
        return true;
    }

}
