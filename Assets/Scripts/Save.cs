using System.Collections;
using System.Collections.Generic;
using System.IO;
using System.Runtime.Serialization.Formatters.Binary;
using UnityEngine;

public class Save : MonoBehaviour
{
    public string filename = "save.dat";
    private string filePath => $"{Application.persistentDataPath}/{filename}";
    private AppData data = new AppData();

    // Start is called before the first frame update
    void Start()
    {
        LoadPositions();
    }

    // Update is called once per frame
    void Update()
    {
        
    }

    public void LoadPositions()
    {
        FileStream file;
 
        if(File.Exists(filePath)) file = File.OpenRead(filePath);
        else
        {
            Debug.LogError($"File not found at: {filePath}");
            return;
        }
        
        BinaryFormatter bf = new BinaryFormatter();
        data = (AppData) bf.Deserialize(file);
        file.Close();
        
        foreach (var pair in data.Transforms)
        {
            var go = GameObject.Find(pair.Key);
            if (go)
            {
                go.transform.localPosition = pair.Value.localPosition;
                go.transform.localRotation = pair.Value.localRotation;
                go.transform.localScale = pair.Value.localScale;
            }
            else
            {
                Debug.LogWarning($"GameObject \"{pair.Key}\" does not exist in scene. Deleting from saved data.");
                data.Transforms.Remove(pair.Key);
            }
        }
    }

    public void SavePosition(GameObject go)
    {
        var sTransform = new SerializableTransform();
        sTransform.localPosition = go.transform.localPosition;
        sTransform.localScale = go.transform.localScale;
        sTransform.localRotation = go.transform.localRotation;
        
        data.Transforms[go.name] = sTransform;
        FileStream file;
 
        if(File.Exists(filePath)) file = File.OpenWrite(filePath);
        else file = File.Create(filePath);
        
        BinaryFormatter bf = new BinaryFormatter();
        bf.Serialize(file, data);
        file.Close();
    }
}
