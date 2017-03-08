/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function ckeditor(ConfigService, CKEditorService) {
  'ngInject';

  return {
    restrict: 'A',
    require: 'ngModel',
    link: (scope, element, attrs, ngModel) => {
      CKEditorService.loadCKEditor().then((CKEDITOR) => {
        // TODO: get editor config from the REST response of the field instead of hard-coding the default config here
/*
         // editor config for formatted text fields:
         const editorConfig = {
         autoUpdateElement: false,
         entities: false,
         basicEntities: true,
         customConfig: '',
         language: ConfigService.locale,
         plugins: 'basicstyles,button,clipboard,contextmenu,divarea,enterkey,entities,floatingspace,floatpanel,htmlwriter,listblock,magicline,menu,menubutton,panel,panelbutton,removeformat,richcombo,stylescombo,tab,toolbar,undo',
         title: false,
         toolbar: [
         { name: 'styles', items: ['Styles'] },
         { name: 'basicstyles', items: ['Bold', 'Italic', 'Underline', '-', 'RemoveFormat'] },
         { name: 'clipboard', items: ['Undo', 'Redo'] },
         ],
         };
*/
        // editor config for rich text fields:
        const editorConfig = {
          autoUpdateElement: false,
          entities: false,
          basicEntities: true,
          customConfig: '',
          dialog_buttonsOrder: 'ltr',
          dialog_noConfirmCancel: true,
          extraAllowedContent: 'embed[allowscriptaccess,height,src,type,width]; img[border,hspace,vspace]; object[align,data,height,id,title,type,width]; p[align]; param[name,value]; table[width]; td[valign,width]; th[valign,width];',
          keystrokes: [
            [CKEDITOR.CTRL + 77, 'maximize'],
            [CKEDITOR.ALT + 66, 'showblocks'],
          ],
          language: ConfigService.locale,
          linkShowAdvancedTab: false,
          plugins: 'a11yhelp,basicstyles,button,clipboard,codemirror,contextmenu,dialog,dialogadvtab,dialogui,divarea,elementspath,enterkey,entities,floatingspace,floatpanel,htmlwriter,indent,indentblock,indentlist,justify,link,list,listblock,liststyle,magicline,maximize,menu,menubutton,panel,panelbutton,pastefromword,pastetext,popup,removeformat,resize,richcombo,showblocks,showborders,specialchar,stylescombo,tab,table,tableresize,tabletools,textselection,toolbar,undo,youtube',
          removeFormatAttributes: 'style,lang,width,height,align,hspace,valign',
          title: false,
          toolbarGroups: [
            { name: 'styles' },
            { name: 'basicstyles' },
            { name: 'undo' },
            { name: 'listindentalign', groups: ['list', 'indent', 'align'] },
            { name: 'links' },
            { name: 'insert' },
            { name: 'tools' },
            { name: 'mode' },
          ],
        };

        const editor = CKEDITOR.replace(element[0], editorConfig);

        ngModel.$render = () => {
          editor.setData(ngModel.$viewValue);
        };

        editor.on('change', () => {
          scope.$evalAsync(() => {
            const html = editor.getData();
            ngModel.$setViewValue(html);
          });
        });

        editor.on('focus', () => {
          element.triggerHandler('focus');
        });

        editor.on('blur', () => {
          element.triggerHandler('blur');
        });

        scope.$on('$destroy', () => {
          editor.destroy();
        });
      });
    },
  };
}

export default ckeditor;

