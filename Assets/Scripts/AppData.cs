using System.Collections.Generic;
using UnityEngine;

[System.Serializable]
public class AppData
{
    public Dictionary<string, SerializableTransform> Transforms = new Dictionary<string, SerializableTransform>();
}

[System.Serializable]
public class SerializableTransform
{
    public SerializableVector3 localPosition;
    public SerializableVector3 localScale;
    public SerializableQuaternion localRotation;
}