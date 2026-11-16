package view;

import javafx.scene.image.Image;
import model.BinType;
import model.TrashKind;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

// Loads your drawn pictures from the "assets" folder (next to src), by file name without the extension.
// A picture that is missing gives null, and GamePanel draws a simple placeholder instead.
public class Assets {
    private final String FOLDER = "assets";
    private final String[] EXTENSIONS = {"png", "jpg", "jpeg"};

    private final Map<String, Image> cache = new HashMap<>();

    public Assets(){
        // load everything once at start-up so the game never stalls on a file later
        get("background");
        for(TrashKind kind : TrashKind.values()){
            getTrash(kind);
        }
        for(BinType type : BinType.values()){
            getBin(type);
        }
        for(int lives = 0; lives <= 3; lives++){
            getTurtle(lives);
        }
    }

    public Image get(String name){
        if(name == null) return null;

        if(!cache.containsKey(name)){
            cache.put(name, load(name));   // a missing picture is remembered as null
        }
        return cache.get(name);
    }

    private Image load(String name){
        for(String ext : EXTENSIONS){
            File file = new File(FOLDER, name + "." + ext);
            if(file.exists()){
                Image image = new Image(file.toURI().toString());
                if(!image.isError()){
                    return image;
                }
            }
        }
        return null;
    }

    public Image getBackground(){
        return get("background");
    }

    public Image getTrash(TrashKind kind){
        Image image = get(kind.getAssetName());
        return image != null ? image : get(kind.getFallbackAsset());
    }

    public Image getBin(BinType type){
        Image image = get(type.getAssetName());
        return image != null ? image : get(type.getFallbackAsset());
    }

    // 3+ lives = fresh turtle, 2 = trashed, 1 = super-trashed, 0 or less = dead turtle
    public Image getTurtle(int lives){
        switch(Math.max(0, Math.min(3, lives))){
            case 3: return get("full-life-turtle");
            case 2: return get("two-life-turtle");
            case 1: return get("one-life-turtle");
            default: return get("game-over-turtle");
        }
    }
}