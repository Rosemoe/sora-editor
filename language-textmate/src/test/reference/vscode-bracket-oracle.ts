/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
import { parseDocument } from 'vscode-source/vs/editor/common/model/bracketPairsTextModelPart/bracketPairsTree/parser.ts';
import { FastTokenizer } from 'vscode-source/vs/editor/common/model/bracketPairsTextModelPart/bracketPairsTree/tokenizer.ts';
import { BracketTokens } from 'vscode-source/vs/editor/common/model/bracketPairsTextModelPart/bracketPairsTree/brackets.ts';
import { DenseKeyProvider } from 'vscode-source/vs/editor/common/model/bracketPairsTextModelPart/bracketPairsTree/smallImmutableSet.ts';
import { LanguageBracketsConfiguration } from 'vscode-source/vs/editor/common/languages/supports/languageBracketsConfiguration.ts';
import { AstNodeKind } from 'vscode-source/vs/editor/common/model/bracketPairsTextModelPart/bracketPairsTree/ast.ts';
import { lengthAdd, lengthToObj } from 'vscode-source/vs/editor/common/model/bracketPairsTextModelPart/bracketPairsTree/length.ts';
const config = { brackets: [['(',')'],['[',']'],['{','}']] };
const tokens = BracketTokens.createFromLanguage({languageId:'test',bracketsNew: new LanguageBracketsConfiguration('test',config)} as any,new DenseKeyProvider());
const inputs = ['{(}{}','(})','{(})','([)]','{[({})]} trailing','({)[])','{{[}}]','x'.repeat(257)+'()'];
let seed = 748891;
const rand = () => (seed = (Math.imul(seed,1664525) + 1013904223) >>> 0);
for (let i = 0; i < 120; i++) {
 let text = ''; const alphabet = '()[]{} abc\n';
 for(let j = 0; j < 80; j++) text += alphabet[rand() % alphabet.length];
 inputs.push(text);
}
const result = inputs.map(text => {
 const root = parseDocument(new FastTokenizer(text,tokens),[],undefined,true);
 const pairs:any[] = [];
 const levels = new Map<string,number>();
 const pos = (offset:any) => {const p=lengthToObj(offset); return [p.lineCount,p.columnCount];};
 function walk(node:any, offset:any, depth:number) {
  if (node.kind === AstNodeKind.Pair) {
   const open=node.openingBracket; const close=node.closingBracket; const body=node.child;
   const equal=levels.get(open.text)||0;
   const closeAt=lengthAdd(lengthAdd(offset,open.length),body?.length||0);
   pairs.push({start:pos(offset),openLength:open.text.length,closeStart:pos(closeAt),closeLength:close?.text.length||0,level:depth,equal,invalid:!close});
   levels.set(open.text,equal+1);
   if(body)walk(body,lengthAdd(offset,open.length),depth+1);
   levels.set(open.text,equal);
  } else if (node.kind === AstNodeKind.List) {
   for(const child of node.children){ if(child){walk(child,offset,depth);offset=lengthAdd(offset,child.length);}}
  } else if(node.kind === AstNodeKind.UnexpectedClosingBracket) {
   pairs.push({start:pos(offset),openLength:lengthToObj(node.length).columnCount,closeStart:pos(offset),closeLength:0,level:depth-1,equal:0,invalid:true});
  }
 }
 walk(root,0,0);
 return {text,pairs};
});
const rows = result.map(({text,pairs}) => JSON.stringify({text,pairs:pairs.map(p => [
 ...p.start,p.openLength,...p.closeStart,p.closeLength,p.level,p.equal,p.invalid
])}));
console.log('[\n' + rows.join(',\n') + '\n]');
