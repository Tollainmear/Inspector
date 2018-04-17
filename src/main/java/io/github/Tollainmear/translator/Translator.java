package io.github.Tollainmear.translator;

import com.google.common.base.Charsets;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.asset.AssetManager;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import io.github.hsyyid.inspector.Inspector;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class Translator {
    private Logger logger;
    private ResourceBundle resourceBundle;
    private AssetManager assetManager;
    private String langPath;
    private File langFile;
    private String lang;
    private Optional<Asset> assetOpt;
    private Asset asset;

    public Translator(Inspector plugin) throws IOException {
        logger = plugin.getLogger();
        assetManager = Sponge.getAssetManager();
        if (plugin.getConfigNode().getNode("inspector").getNode("Language").isVirtual()){
            plugin.getConfigNode().getNode("inspector").getNode("Language").setValue("zh_CN");
            lang = "zh_CN";
        }else lang = plugin.getConfigNode().getNode("inspector").getNode("Language").getString();
        langPath = plugin.getConfigPath().toString() + "/lang/";
        assetOpt = assetManager.getAsset(plugin, "lang/" + lang + ".properties");
        if (!(Files.exists(Paths.get(langPath)))) {
            Files.createDirectory(Paths.get(langPath));
        }
        if (!(langFile = new File(langPath + lang + ".properties")).exists()) {
            if (!assetOpt.isPresent()) {




                plugin.getConfigNode().getNode("KaroglanSignEditor").getNode("Language").setValue("zh_CN");
                plugin.getConfigLoader().save(plugin.getConfigNode());
                assetOpt = assetManager.getAsset(plugin, "lang/zh_CN.properties");
                if (!assetOpt.isPresent()) {
                    logger.warn("Ops....Could not load en_US else,please submit issues at:");
                    logger.warn("https://github.com/Tollainmear/KaroglanSignEditor/issues");
                    return;
                }
                langFile = new File(langPath + "en_US.properties");
                if (!(Files.exists(Paths.get(langPath + "en_US.properties")))){
                    assetOpt.get().copyToFile(langFile.toPath());
                    logger.info("Release Language file successfully.");
                }
            } else {
                if (!(Files.exists(langFile.toPath()))){
                    assetOpt.get().copyToFile(langFile.toPath());
                    logger.info("Release Language file successfully.");
                }
            }
        }
        resourceBundle = new PropertyResourceBundle(new InputStreamReader(langFile.toURI().toURL().openStream(), Charsets.UTF_8));
        logInfo("LanguageLoaded");
        asset = assetOpt.get();
        return;
    }

    private void srcNotFound(){
        logger.warn("Could not found the Language file\"" + lang + "\",Inspector will using the \"zh_CN\" by default");
        logger.info("You could also upload your Language resource library at here :");
        logger.info("https://github.com/Tollainmear/KaroglanSignEditor/tree/master/resources/assets/karoglansigneditor/lang");
    }

    public void checkUpdate() throws IOException {
        File newFile;
        String verNow = getstring("version");
        if (verNow == null) verNow = "1.0";
        String verInPackage = new PropertyResourceBundle(new InputStreamReader(asset.getUrl().openStream(), Charsets.UTF_8))
                .getString("version");
        if (verInPackage.compareTo(verNow) > 0) {
            if ((newFile = new File(langPath + lang + "_old.properties")).exists()) {
                newFile.delete();
            }
            new File(langFile.toString()).renameTo(newFile);
            asset.copyToDirectory(Paths.get(langPath));
            resourceBundle = new PropertyResourceBundle(new InputStreamReader(langFile.toURI().toURL().openStream(), Charsets.UTF_8));
            logInfo("Language.Updated");
            return;
        }
        logInfo("Language.NeedNotUpdate");
    }

    public String getstring(String key) {
        if (key == null) {
            return "Language resource Not Found";
        }
        if (resourceBundle.getString(key) == null) {
            return "&c[Language not found]";
        }
        try {

            return resourceBundle.getString(key);
        }
        catch (MissingResourceException e)
        {
            return "&c[MissingLanguage]";
        }
    }

    public Text getText(String key) {
        if (key == null) {
            return TextSerializers.FORMATTING_CODE.deserialize("Language Not Found");
        }
        return TextSerializers.FORMATTING_CODE.deserialize(getstring(key));
    }

    public void logInfo(String key) {
        logger.info("\033[36m" + getstring(key));
    }

    public void logWarn(String key) {
        logger.info("\031[36m" + getstring(key));
    }

}
