#include <netdb.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
const char MYPORT[] = "5556";   // the port users will be connecting to
const int MAXBUFLEN = 200;
//                   XxxxxXXXXxxxxXXXXxxxxXXXXxxxx
const int REFKEY = 0b10001010101000000000000000000;
// get sockaddr, IPv4 or IPv6:
void *get_in_addr(struct sockaddr *sa)
{
    if (sa->sa_family == AF_INET) {
        return &(((struct sockaddr_in*)sa)->sin_addr);
    }
    return &(((struct sockaddr_in6*)sa)->sin6_addr);
}

enum {PURE_FL, PURE_INT, PURE_STR, BASIC_CMD} para_type;

void interpret(char* in){
    FILE* outto = stdout;
    union{
        int myint;
        float myfloat;
    }val;
    char *head, *count, *data, *id;
    int ii;
    //parse the header and the sequence number============================
    head = strtok(in, "=");
    if( !(head[0]=='A' && head[1]=='T' && head[2]=='*') )
        perror("Wrong Header, seems NOT ARDrone COMMAND");
    head+=3;
    int para_num = 0;
    if( !strcmp(head, "PCMD") )
    {
        fprintf(outto, "[MOVE4]\t");
        fflush(outto);
        para_type = PURE_FL;
        para_num = 4;
    }
    else if( !strcmp(head, "CONFIG_IDS") )
    {
        fprintf(outto, "[CONFIG_ID]\t");
        fflush(outto);
        para_type = PURE_STR;
        para_num = 3;
    }
    else if( !strcmp(head, "REF") )
    {
        fprintf(outto, "[BASIC_CMD]\t");
        fflush(outto);
        para_type = BASIC_CMD;
    }
    else
    {
        fprintf(stderr, "Unknown header \"%s\"\n", head);
        fflush(outto);
    }
    count = strtok(NULL, ",");
    fprintf(outto, "#%s, parameter:(", count);
    fflush(outto);
    
    //parse the parameters=================================================
    if(para_type == PURE_FL)
    {
        data = strtok(NULL, "\"");
        float *paras = NULL;
        paras = malloc( para_num * sizeof(float) );
        char *end = NULL;
        for(ii=0; ii<para_num; ii++)
        {
            val.myint = strtol(data, &end, 10);
            paras[ii]=val.myfloat;
            if(ii!=0) fprintf(outto, ",");
            fflush(outto);
            fprintf(outto, "%f", paras[ii]);
            fflush(outto);
            data = end+1;
        }
    }
    else if(para_type == PURE_STR)
    {
        data = strtok(NULL, ",");
        //printf("%s\n", data);
        char **paras = NULL;
        paras = malloc( para_num * sizeof(char*) );
        char *end = NULL;
        paras[0] = data;
        fprintf(outto, "%s", paras[0]);
        fflush(outto);
        for(ii=1; ii<para_num; ii++)
        {
            paras[ii] = strtok(NULL, ",");
            if(ii!=0) fprintf(outto, ",");
            fflush(outto);
            fprintf(outto, "%s", paras[ii]);
            fflush(outto);
        }
    }
    else if(para_type == BASIC_CMD)
    {
        data = strtok(NULL, "\"");
        val.myint = strtol(data, NULL, 10);
        if( (val.myint & REFKEY) != REFKEY || ((val.myint & (~0x300)) & (~REFKEY)) != 0)
        {
            fprintf(stderr, "data=%d\n", val.myint);
            fflush(outto);
            perror("wrong formatting");
        }
        else
        {
            if( (val.myint & 0x100) !=0)
                fprintf(outto, "Emergency");
            else if( (val.myint & 0x200) !=0)
                fprintf(outto, "Take Off");
            fflush(outto);
        }
    }
    fprintf(outto, ")\n");
    fflush(outto);
}


int main(void)
{
    char test[]="AT*PCMD=598562,1,1016225096,-2147483648,0,0\"aabbccdd\"";
    interpret(test);
    int sockfd;
    struct addrinfo hints, *servinfo, *p;
    int rv;
    int numbytes;
    struct sockaddr_storage their_addr;
    char buf[MAXBUFLEN];
    socklen_t addr_len;
    char s[INET6_ADDRSTRLEN];
    memset(&hints, 0, sizeof hints);
    hints.ai_family = AF_INET; // set to AF_INET to force IPv4
    hints.ai_socktype = SOCK_DGRAM;
    hints.ai_flags = AI_PASSIVE; // use my IP
    if ((rv = getaddrinfo(NULL, MYPORT, &hints, &servinfo)) != 0) {
        fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(rv));
        return 1;
    }
    // loop through all the results and bind to the first we can
    for(p = servinfo; p != NULL; p = p->ai_next) {
        if ((sockfd = socket(p->ai_family, p->ai_socktype,
                             p->ai_protocol)) == -1) {
            perror("listener: socket");
            continue; }
        if (bind(sockfd, p->ai_addr, p->ai_addrlen) == -1) {
            close(sockfd);
            perror("listener: bind");
            continue;
        }
        break;
    }
    if (p == NULL) {
        fprintf(stderr, "listener: failed to bind socket\n");
        return 2;
    }
    freeaddrinfo(servinfo);
    while(1){
        //printf("listener: waiting to recvfrom...\n");
        addr_len = sizeof their_addr;
        if ((numbytes = recvfrom(sockfd, buf, MAXBUFLEN-1 , 0,
                                 (struct sockaddr *)&their_addr, &addr_len)) == -1) {
            perror("recvfrom");
            exit(1);
        }
        //printf("listener: recvfrom finished.\n");
        //    printf("listener: got packet from %s\n",
        //        inet_ntop(their_addr.ss_family,
        //            get_in_addr((struct sockaddr *)&their_addr),
        //            s, sizeof(s) ) );
        fprintf(stderr, "listener: packet is %d bytes long\n", numbytes);
        buf[numbytes] = '\0';
        //puts("listener: packet contains");
        //puts(buf);
        interpret(buf);
        //usleep(100000);
    }
    close(sockfd);
    return 0;
}

