using System.Collections;
using System.Collections.Generic;
using UnityEngine;



public class OscManager : MonoBehaviour
{
    public string RemoteIpAddress;
    public int PortOutMaxMsp;
    public int PortInMaxMsp;

    public OscIn OscInMaxMsp;
    public OscOut OscOutMaxMsp;
    
    // Start is called before the first frame update
    void Start()
    {
        if (RemoteIpAddress.Length > 0)
        {
            if (PortOutMaxMsp > 0)
            {
                OscOutMaxMsp = gameObject.AddComponent<OscOut>();
                OscOutMaxMsp.Open(PortOutMaxMsp, RemoteIpAddress);    
            } else Debug.LogWarning("PortOutMaxMsp not defined");
            
            if (PortInMaxMsp > 0)
            {
                OscInMaxMsp = gameObject.AddComponent<OscIn>();
                OscInMaxMsp.Open(PortInMaxMsp);    
            } else Debug.LogWarning("PortOutMaxMsp not defined");
                
        }
        else Debug.LogWarning("OscManager needs a RemoteIpAddress");
        
    }

    // Update is called once per frame
    void Update()
    {
        
    }
}
