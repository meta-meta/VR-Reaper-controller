using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

//[ExecuteInEditMode]
public class ListenerIEM : MonoBehaviour
{
    public int PortIn;
    public int PortOut;
    public int PortOutSceneRotator;

    private GameObject _nametag;

    // The GameObject with an AudioObjectIEM we will treat as the "master" for room controls
//    public GameObject RoomMaster;

    private OscIn _oscInRoomEncoder;
    private OscOut _oscOutRoomEncoder;
    private OscOut _oscOutSceneRotator;

    private Quaternion _rot;
    private Vector3 _pos;

    private OscMessage _sceneRotatorQuaternions;
    
    private OscMessage _listenerX;
    private OscMessage _listenerY;
    private OscMessage _listenerZ;

    private GameObject ambisonicVisualizer;
    
    private void Start()
    {
        var ip = GameObject.Find("OSC").GetComponent<OscManager>().RemoteIpAddress;

        if (PortOut > 0)
        {
            _oscOutRoomEncoder = gameObject.AddComponent<OscOut>();
            _oscOutRoomEncoder.Open(PortOut, ip);
            
            _listenerX = new OscMessage($"/RoomEncoder/listenerX");
            _listenerY = new OscMessage($"/RoomEncoder/listenerY");
            _listenerZ = new OscMessage($"/RoomEncoder/listenerZ");
        }
        
        if (PortOutSceneRotator > 0)
        {
            _oscOutSceneRotator = gameObject.AddComponent<OscOut>();
            _oscOutSceneRotator.Open(PortOutSceneRotator, ip);
            
            _sceneRotatorQuaternions = new OscMessage($"/SceneRotator/quaternions");
        }

        if (PortIn > 0)
        {
            _oscInRoomEncoder = gameObject.AddComponent<OscIn>();
            _oscInRoomEncoder.Open(PortIn);
            
            _oscInRoomEncoder.MapFloat($"/RoomEncoder/listenerX", OnReceiveListenerX );
            _oscInRoomEncoder.MapFloat($"/RoomEncoder/listenerY", OnReceiveListenerY );
            _oscInRoomEncoder.MapFloat($"/RoomEncoder/listenerZ", OnReceiveListenerZ );
        }

        ambisonicVisualizer = GameObject.Find("AmbisonicVisualizer");
    }

    void OnReceiveListenerX(float val)
    {
        transform.position = new Vector3(transform.position.x,transform.position.y,val);
    }
    
    void OnReceiveListenerY(float val) // IEM Y = Unity Z
    {
        transform.position = new Vector3(-val,transform.position.y,transform.position.z);
    }
    
    void OnReceiveListenerZ(float val) // IEM Z = Unity Y
    {
        transform.position = new Vector3(transform.position.x,val,transform.position.z);
    }

    private string _name;
    private void UpdateNametag()
    {
        if (_name != gameObject.name)
        {
            if (!_nametag) _nametag = transform.Find("Nametag").gameObject;
            _name = gameObject.name;
            _nametag.GetComponent<TextMesh>().text = _name;
        }
    }

    static float Scale(float inMin, float inMax, float outMin, float outMax, float val) =>
        Mathf.Lerp(
            outMin,
            outMax,
            Mathf.InverseLerp(inMin, inMax, val));

    private void SendPositionToRoomEncoder() // TODO: move this script to AmbisonicVisualizer and make that the canonical listener tool.
    {
        _pos = transform.position;
        _listenerX.Set(0, _pos.z);
        _listenerY.Set(0, -_pos.x);
        _listenerZ.Set(0, _pos.y);
        _oscOutRoomEncoder.Send(_listenerX);
        _oscOutRoomEncoder.Send(_listenerY);
        _oscOutRoomEncoder.Send(_listenerZ);

        _rot = ambisonicVisualizer.transform.rotation * transform.rotation;
        
        _sceneRotatorQuaternions.Set(0, _rot.w);
        _sceneRotatorQuaternions.Set(1, _rot.z);
        _sceneRotatorQuaternions.Set(2, -_rot.x);
        _sceneRotatorQuaternions.Set(3, _rot.y);
        
        _oscOutSceneRotator.Send(_sceneRotatorQuaternions);
    }
    
    void Update()
    {
        SendPositionToRoomEncoder();
    }
}