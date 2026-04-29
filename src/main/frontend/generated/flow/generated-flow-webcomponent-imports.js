import { injectGlobalWebcomponentCss } from 'Frontend/generated/jar-resources/theme-util.js';

import { injectGlobalCss } from 'Frontend/generated/jar-resources/theme-util.js';

import { css, unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin';
import $cssFromFile_3 from '@vaadin/vaadin-lumo-styles/lumo.css?inline';
import $cssFromFile_6 from 'Frontend/styles/loginView.css?inline';
import $cssFromFile_7 from 'Frontend/styles/checking.css?inline';
import '@vaadin/vertical-layout/src/vaadin-vertical-layout.js';
import '@vaadin/login/src/vaadin-login-form.js';
import '@vaadin/select/src/vaadin-select.js';
import 'Frontend/generated/jar-resources/selectConnector.js';
import '@vaadin/tooltip/src/vaadin-tooltip.js';
import 'Frontend/generated/jar-resources/flow-component-renderer.js';
import 'Frontend/generated/jar-resources/flow-component-directive.js';
import 'lit';
import 'Frontend/generated/jar-resources/lit-renderer.ts';
import 'lit/directives/live.js';
import '@vaadin/multi-select-combo-box/src/vaadin-multi-select-combo-box.js';
import 'Frontend/generated/jar-resources/comboBoxConnector.js';
import '@vaadin/component-base/src/debounce.js';
import '@vaadin/component-base/src/async.js';
import '@vaadin/combo-box/src/vaadin-combo-box-placeholder.js';
import '@vaadin/grid/src/vaadin-grid.js';
import '@vaadin/grid/src/vaadin-grid-column.js';
import '@vaadin/grid/src/vaadin-grid-sorter.js';
import '@vaadin/checkbox/src/vaadin-checkbox.js';
import 'Frontend/generated/jar-resources/gridConnector.ts';
import '@vaadin/grid/src/vaadin-grid-active-item-mixin.js';
import 'Frontend/generated/jar-resources/vaadin-grid-flow-selection-column.js';
import '@vaadin/grid/src/vaadin-grid-column-group.js';
import '@vaadin/context-menu/src/vaadin-context-menu.js';
import 'Frontend/generated/jar-resources/contextMenuConnector.js';
import 'Frontend/generated/jar-resources/contextMenuTargetConnector.js';
import '@vaadin/component-base/src/gestures.js';
import 'Frontend/generated/jar-resources/disableOnClickFunctions.js';
import '@vaadin/upload/src/vaadin-upload.js';
import '@vaadin/button/src/vaadin-button.js';
import '@vaadin/text-field/src/vaadin-text-field.js';
import '@vaadin/horizontal-layout/src/vaadin-horizontal-layout.js';
import '@vaadin/dialog/src/vaadin-dialog.js';
import '@vaadin/icons/vaadin-iconset.js';
import '@vaadin/icon/src/vaadin-icon.js';
import '@vaadin/notification/src/vaadin-notification.js';
import '@vaadin/common-frontend/ConnectionIndicator.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';
import 'react-router';
import 'react';

injectGlobalWebcomponentCss($cssFromFile_3.toString());
injectGlobalWebcomponentCss($cssFromFile_6.toString());
injectGlobalWebcomponentCss($cssFromFile_7.toString());
const loadOnDemand = (key) => {
  const pending = [];
  if (key === 'a111f0738e0b4f951810c32a6538b19e718b2ffa2c916631203db8d4f9af52ad') {
    pending.push(import('./chunks/chunk-36203f35b6867fd6be316f36bef36f7a1bb89b56caeca0de9ba2d0c34f348e30.js'));
  }
  return Promise.all(pending);
}
window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}