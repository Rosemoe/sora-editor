/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2025  Rosemoe
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
package io.github.rosemoe.sora.app.lsp;

import static io.github.rosemoe.sora.app.UtilsKt.switchThemeIfRequired;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.WorkspaceFoldersChangeEvent;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.tm4e.core.registry.IThemeSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.github.rosemoe.sora.app.BaseEditorActivity;
import io.github.rosemoe.sora.app.R;
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme;
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage;
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry;
import io.github.rosemoe.sora.langs.textmate.registry.dsl.LanguageDefinitionListBuilder;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver;
import io.github.rosemoe.sora.lsp.client.connection.LocalSocketStreamConnectionProvider;
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition;
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.EventHandler;
import io.github.rosemoe.sora.lsp.editor.LspEditor;
import io.github.rosemoe.sora.lsp.editor.LspProject;
import io.github.rosemoe.sora.text.ContentIO;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import kotlin.Unit;

public class LspTestJavaActivity extends BaseEditorActivity {
    private volatile LspEditor lspEditor;
    private volatile LspProject lspProject;
    private Menu rootMenu;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("LSP Test - Java");

        var font = Typeface.createFromAsset(getAssets(), "JetBrainsMono-Regular.ttf");

        editor.setTypefaceLineNumber(font);
        editor.setTypefaceText(font);

        try {
            ensureTextmateTheme();
            switchThemeIfRequired(this, editor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ForkJoinPool.commonPool().execute(() -> {
            try {
                unAssets();
                connectToLanguageServer();
                setEditorText();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }


    private void setEditorText() throws IOException {
        var file = new File(getExternalCacheDir(), "testProject/sample.lua");

        var text = ContentIO.createFrom(new FileInputStream(file));

        runOnUiThread(() -> {
            editor.setText(text, null);
            editor.getComponent(EditorAutoCompletion.class).setEnabledAnimation(true);
        });

    }


    private void connectToLanguageServer() throws IOException, InterruptedException {
        runOnUiThread(() -> {
            toast("(Java Activity) Starting Language Server...");
            editor.setEditable(false);
        });

        var projectPath = new File(getExternalCacheDir(), "testProject").getAbsolutePath();

        var intent = new Intent(this, LspLanguageServerService.class);

        startService(
                intent
        );

        var luaServerDefinition =
                new CustomLanguageServerDefinition("lua",
                        workingDir -> new LocalSocketStreamConnectionProvider("lua-lsp")) {

                    private final EventListener eventListener = new EventListener(LspTestJavaActivity.this);


                    @NonNull
                    @Override
                    public EventHandler.EventListener getEventListener() {
                        return eventListener;
                    }
                };

        lspProject = new LspProject(projectPath);

        lspProject.addServerDefinition(luaServerDefinition);

        final Object lock = new Object();

        runOnUiThread(() -> {
            lspEditor = lspProject.createEditor(projectPath + "/sample.lua");

            var wrapperLanguage = createTextMateLanguage();
            lspEditor.setWrapperLanguage(wrapperLanguage);
            lspEditor.setEditor(editor);
            lspEditor.setEnableInlayHint(true);

            synchronized (lock) {
                lock.notify();
            }
        });


        synchronized (lock) {
            lock.wait();
        }

        boolean connected;

        // delay(Timeout[Timeouts.INIT].toLong()) //wait for server start

        try {
            lspEditor.connectWithTimeoutBlocking();

            var changeWorkspaceFoldersParams = new DidChangeWorkspaceFoldersParams();

            changeWorkspaceFoldersParams.setEvent(new WorkspaceFoldersChangeEvent());

            changeWorkspaceFoldersParams.getEvent().setAdded(List.of(new WorkspaceFolder("file://" + projectPath + "/std/Lua53", "MyLuaProject")));

            Objects.requireNonNull(lspEditor.getRequestManager())
                    .didChangeWorkspaceFolders(
                            changeWorkspaceFoldersParams
                    );

            connected = true;

        } catch (Exception e) {
            connected = false;
            e.printStackTrace();
        }

        boolean finalConnected = connected;

        runOnUiThread(() -> {
            if (finalConnected) {
                toast("Initialized Language server");
            } else {
                toast("Unable to connect language server");
            }
            editor.setEditable(true);
        });
    }


    private void toast(String text) {
        Toast.makeText(
                this,
                text,
                Toast.LENGTH_SHORT
        ).show();
    }

    private TextMateLanguage createTextMateLanguage() {

        var builder = new LanguageDefinitionListBuilder();

        builder.language("lua", languageDefinitionBuilder -> {
            languageDefinitionBuilder.setGrammar("textmate/lua/syntaxes/lua.tmLanguage.json");
            languageDefinitionBuilder.setScopeName("source.lua");
            languageDefinitionBuilder.setLanguageConfiguration("textmate/lua/language-configuration.json");
            return Unit.INSTANCE;
        });

        GrammarRegistry.getInstance().loadGrammars(
                builder.build()
        );

        return TextMateLanguage.create(
                "source.lua", false
        );
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        switchThemeIfRequired(this, editor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_lsp, menu);
        rootMenu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        var id = item.getItemId();
        if (id == R.id.code_format) {
            var cursor = editor.getText().getCursor();
            if (cursor.isSelected()) {
                editor.formatCodeAsync(cursor.left(), cursor.right());
            } else {
                editor.formatCodeAsync();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onDestroy() {
        super.onDestroy();

        editor.release();
        try {
            ForkJoinPool.commonPool().execute(() -> {
                lspEditor.dispose();
                lspProject.dispose();
            });
            stopService(new Intent(LspTestJavaActivity.this, LspLanguageServerService.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void unAssets() throws IOException {
        ZipFile zipFile = new ZipFile(getPackageResourcePath());
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        while (zipEntries.hasMoreElements()) {
            ZipEntry zipEntry = zipEntries.nextElement();
            String fileName = zipEntry.getName();
            if (fileName.startsWith("assets/testProject/")) {
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                //The compiler will be optimized here, don't worry
                File filePath = new File(getExternalCacheDir(), fileName.substring("assets/".length()));
                filePath.getParentFile().mkdirs();
                FileOutputStream outputStream = new FileOutputStream(filePath);

                byte[] buffer = new byte[1024];

                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }

                inputStream.close();
                outputStream.close();
            }
        }
        zipFile.close();

    }

    private void ensureTextmateTheme() throws Exception {
        var editorColorScheme = editor.getColorScheme();

        if (editorColorScheme instanceof TextMateColorScheme) {
            return;
        }

        FileProviderRegistry.getInstance().addFileProvider(
                new AssetsFileResolver(
                        getAssets()
                )
        );

        var themeRegistry = ThemeRegistry.getInstance();

        var path = "textmate/quietlight.json";


        themeRegistry.loadTheme(
                new ThemeModel(
                        IThemeSource.fromInputStream(
                                FileProviderRegistry.getInstance().tryGetInputStream(path), path, null
                        ), "quitelight"
                )
        );

        themeRegistry.setTheme("quietlight");

        editorColorScheme = TextMateColorScheme.create(themeRegistry);

        editor.setColorScheme(editorColorScheme);

    }

    class EventListener implements EventHandler.EventListener {
        private WeakReference<LspTestJavaActivity> ref;

        public EventListener(LspTestJavaActivity activity) {
            this.ref = new WeakReference<>(activity);
        }

        @Override
        public void initialize(@Nullable LanguageServer server, @NonNull InitializeResult result) {


            var activity = ref.get();

            if (activity == null) {
                return;
            }

            activity.runOnUiThread(() -> {
                var item = rootMenu.findItem(R.id.code_format);

                var isEnabled =
                        result.getCapabilities().getDocumentFormattingProvider() != null;

                item.setEnabled(isEnabled);
            });

        }

        @Override
        public void onHandlerException(@NonNull Exception exception) {

        }

        @Override
        public void onShowMessage(@Nullable MessageParams messageParams) {

        }

        @Override
        public void onLogMessage(@Nullable MessageParams messageParams) {

        }
    }
}


